package org.dxworks.jiraminer.main;

import com.google.api.client.http.HttpRequestInitializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dxworks.jiraminer.LocalDateFormatter;
import org.dxworks.jiraminer.cache.CacheDTO;
import org.dxworks.jiraminer.cache.CacheRepository;
import org.dxworks.jiraminer.configuration.ExportType;
import org.dxworks.jiraminer.configuration.JiraMinerConfiguration;
import org.dxworks.jiraminer.configuration.JiraMinerConfigurer;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.deployment.JiraDeploymentContextFactory;
import org.dxworks.jiraminer.dto.response.issues.*;
import org.dxworks.jiraminer.export.JsonFileWriter;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.export.DetailedResultMapper;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssueFieldsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

import static org.dxworks.jiraminer.cache.CacheRepository.merge;

@Slf4j
public class Main {

	private static JiraMinerConfigurer jiraMinerConfigurer;
    private static final CacheRepository cacheRepository = new CacheRepository();
	private static final LocalDate now = LocalDate.now();
    private static final Map<String, String> fieldNamesById = new HashMap<>();
	private static final List<IssueField> customFields = new ArrayList<>();
	private static final BasicResultMapper BASIC_RESULT_MAPPER = new BasicResultMapper();
	private static final JsonFileWriter jsonFileWriter = new JsonFileWriter();
	private static boolean fieldNamesLoaded = false;

	public static void main(String[] args) {
		log.info("Starting Jira Miner...");

        JiraMinerConfiguration jiraMinerConfiguration = JiraMinerConfiguration.getInstance();

		// Detect deployment type before any issue retrieval - fail fast if detection fails
		HttpRequestInitializer requestInitializer = JiraMinerConfigurer.getAuthenticator(jiraMinerConfiguration);
		JiraDeploymentContext deploymentContext = JiraDeploymentContextFactory.detect(jiraMinerConfiguration.getJiraHome(), requestInitializer);
		log.info("Detected Jira deployment type: {}", deploymentContext.getDeploymentType());

		ExportType exportType = jiraMinerConfiguration.needsDetailedExport() ? ExportType.DETAILED : ExportType.BASIC;
		try (JiraMinerConfigurer configurer = new JiraMinerConfigurer(jiraMinerConfiguration, deploymentContext, exportType)) {
			jiraMinerConfigurer = configurer;
			ImmutablePair<List<Issue>, List<IssueStatus>> issuesAndStatuses = null;

			try {
				issuesAndStatuses = getIssuesAndStatusesCaching(jiraMinerConfiguration);
			} catch (Exception e) {
				log.error("Error getting issues", e);
			}

			if (issuesAndStatuses == null) {
				log.error("Skipping export because issues could not be retrieved.");
				return;
			}

			log.info("Writing results to file...");
			ensureResultsFolderExists();
			String projectID = jiraMinerConfiguration.getProjectId();
			writeBasicIssuesToFile(projectID, issuesAndStatuses.left);

			if (jiraMinerConfiguration.needsDetailedExport()) {
				DetailedResultMapper resultExporter = new DetailedResultMapper(deploymentContext);
				jsonFileWriter.write(
						getOutputFIle(projectID + "-detailed"),
						resultExporter.getExportResult(issuesAndStatuses.left, issuesAndStatuses.right, customFields)
				);
			}
		}
		log.info("Finished Jira Miner.");
	}

	private static ImmutablePair<List<Issue>, List<IssueStatus>> getIssuesAndStatusesCaching(JiraMinerConfiguration jiraMinerConfiguration) {
        IssuesService issuesService = jiraMinerConfigurer.configureIssuesService();
        CommentsService commentsService = jiraMinerConfigurer.configureCommentsService();
        StatusesService statusesService = jiraMinerConfigurer.configureStatusesService();
        IssueFieldsService issueFieldsService = jiraMinerConfigurer.configureIssueFieldsService();
		loadFieldNames(issueFieldsService);
		String projectId = jiraMinerConfiguration.getProjectId();

		CacheDTO cacheDTO;
		List<Issue> issues;
		if (jiraMinerConfiguration.useCache() && (cacheDTO = cacheRepository.read(projectId)) != null) {
			List<Issue> newIssues = getIssues(jiraMinerConfiguration, issuesService, commentsService, LocalDateFormatter.parse(cacheDTO.getAt()));
			issues = merge(cacheDTO.getIssues(), newIssues);
		} else {
			issues = getIssues(jiraMinerConfiguration, issuesService, commentsService, null);
		}
		List<IssueStatus> allStatuses = statusesService.getAllStatuses();

		if(jiraMinerConfiguration.useCache())
			cacheRepository.cache(projectId, issues, allStatuses, Main.now);

		return new ImmutablePair<>(issues, allStatuses);
	}

	private static List<Issue> getIssues(JiraMinerConfiguration jiraMinerConfiguration,
										 IssuesService issuesService,
										 CommentsService commentsService,
										 LocalDate updatedAfter) {
		List<Issue> newIssues = issuesService.getAllIssuesForProjects(updatedAfter, null, jiraMinerConfiguration.getProjects());
		if(jiraMinerConfiguration.needsDetailedExport()) {
			newIssues.forEach(issuesService::addChangeLog);
			commentsService.addCommentsToIssues(newIssues);
		}
		return newIssues;
	}

	private static void ensureResultsFolderExists() {
		File directory = new File("results");
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	private static void writeBasicIssuesToFile(String projectID, List<Issue> issues) {
		JiraMinerConfiguration jiraMinerConfiguration = JiraMinerConfiguration.getInstance();
		boolean exportCustomFields = CustomFieldsExportConfig.shouldExportCustomFields(
				jiraMinerConfiguration.getProperty(CustomFieldsExportConfig.EXPORT_CUSTOM_FIELDS)
		);
		jsonFileWriter.write(
				getOutputFIle(projectID),
				BASIC_RESULT_MAPPER.getExportResult(issues, fieldNamesById, exportCustomFields)
		);
	}

	private static File getOutputFIle(String projectID) {
		return new File("results/" + projectID + "-issues.json");
	}

	private static void loadFieldNames(IssueFieldsService fieldService) {
		if (fieldNamesLoaded) {
			return;
		}
		
		fieldNamesById.clear();
		customFields.clear();
		
		if (fieldService != null) {
			try {
				List<IssueField> issueFields = fieldService.getFields();
				if (issueFields != null) {
					for (IssueField field : issueFields) {
						if (field == null) {
							continue;
						}
						fieldNamesById.put(field.getId(), field.getName());
						if (field.isCustom() || (field.getId() != null && field.getId().startsWith("customfield_"))) {
							customFields.add(field);
						}
					}
				}
				log.info("Loaded {} field names from Jira", fieldNamesById.size());
			} catch (Exception e) {
				log.warn("Could not fetch field names from Jira", e);
			}
		}
		
		fieldNamesLoaded = true;
	}

}
