package org.dxworks.jiraminer.main;

import com.google.api.client.http.HttpResponseException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.dxworks.jiraminer.LocalDateFormatter;
import org.dxworks.jiraminer.cache.CacheDTO;
import org.dxworks.jiraminer.cache.CacheRepository;
import org.dxworks.jiraminer.configuration.JiraMinerConfiguration;
import org.dxworks.jiraminer.configuration.JiraMinerConfigurer;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.JiraComponent;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.export.ResultExporter;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;
import org.dxworks.utils.java.rest.client.utils.JsonMapper;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.dxworks.jiraminer.cache.CacheRepository.merge;

@Slf4j
public class Main {

	public static final String BASIC = "basic";
	public static final String DETAILED = "detailed";
	private static JiraMinerConfigurer jiraMinerConfigurer;
	private static final String afterPrefix = "-after=";
	private static final String beforePrefix = "-before=";
	private static final CacheRepository cacheRepository = new CacheRepository();
	private static final LocalDate now = LocalDate.now();


	public static void main(String[] args) {
		log.info("Starting Jira Miner...");

		JiraMinerConfiguration jiraMinerConfiguration = JiraMinerConfiguration.getInstance();
		jiraMinerConfigurer = new JiraMinerConfigurer(jiraMinerConfiguration);
		ImmutablePair<List<Issue>, List<IssueStatus>> issuesAndStatuses = null;

		try {
			issuesAndStatuses = getIssuesAndStatusesCaching(jiraMinerConfiguration);
		} catch (Exception e) {
			if (e instanceof HttpResponseException) {
				HttpResponseException exception = (HttpResponseException) e;
				int statusCode = exception.getStatusCode();
				if (statusCode == 401 || statusCode == 404) {
					log.error("Error retrieving issues. Please revise your authentication method and try again. \n"
							+ "If you are using cookie based authentication, please renew the cookie!", e);
					System.exit(1);
				}
			}
			log.error("Error getting issues", e);
		}

		log.info("Writing results to file...");
		ensureResultsFolderExists();
		List<String> exportTypes = Optional.ofNullable(jiraMinerConfiguration.getProperty("exportTypes"))
				.map(types -> asList(types.split(","))).orElse(singletonList(BASIC));
		String projectID = jiraMinerConfiguration.getProjectId();
		if (exportTypes.contains(BASIC)) {
			writeBasicIssuesToFile(projectID, issuesAndStatuses.left);
		}
		if (exportTypes.contains(DETAILED)) {
			new ResultExporter().export(issuesAndStatuses.left, issuesAndStatuses.right, getOutputFIle(projectID + "-detailed"));
		}
		log.info("Finished Jira Miner.");
	}

	private static ImmutablePair<List<Issue>, List<IssueStatus>> getIssuesAndStatusesCaching(JiraMinerConfiguration jiraMinerConfiguration) {
		IssuesService issuesService = jiraMinerConfigurer.configureIssuesService();
		CommentsService commentsService = jiraMinerConfigurer.configureCommentsService();
		StatusesService statusesService = jiraMinerConfigurer.configureStatusesService();
		String projectId = jiraMinerConfiguration.getProjectId();

		CacheDTO cacheDTO = cacheRepository.read(projectId);
		List<Issue> issues;
		if (cacheDTO != null) {
			List<Issue> newIssues = getIssues(jiraMinerConfiguration, issuesService, commentsService, LocalDateFormatter.parse(cacheDTO.getAt()));
			issues = merge(cacheDTO.getIssues(), newIssues);
		} else {
			issues = getIssues(jiraMinerConfiguration, issuesService, commentsService, null);
		}
		List<IssueStatus> allStatuses = statusesService.getAllStatuses();

		cacheRepository.cache(projectId, issues, allStatuses, Main.now);
		return new ImmutablePair<>(issues, allStatuses);
	}

	private static List<Issue> getIssues(JiraMinerConfiguration jiraMinerConfiguration,
										 IssuesService issuesService,
										 CommentsService commentsService,
										 LocalDate updatedAfter) {
		List<Issue> newIssues = issuesService.getAllIssuesForProjects(updatedAfter, null, jiraMinerConfiguration.getProjects());
		newIssues.forEach(commentsService::addCommentsToIssue);
		return newIssues;
	}

	private static void ensureResultsFolderExists() {
		File directory = new File("results");
		if (!directory.exists()) {
			directory.mkdirs();
		}
	}

	@SneakyThrows
	private static void writeBasicIssuesToFile(String projectID, List<Issue> issues) {
		new JsonMapper().writeJSON(new FileWriter(getOutputFIle(projectID)), toBasicJiraMinerOutput(issues));
	}

	private static File getOutputFIle(String projectID) {
		return new File("results/" + projectID + "-issues.json");
	}

	private static List<BasicJiraMinerOutput> toBasicJiraMinerOutput(List<Issue> issues) {
		return issues.stream()
				.map(issue -> BasicJiraMinerOutput.builder().key(issue.getKey()).summary(issue.getSummary())
						.description(issue.getDescription()).status(issue.getStatus().getName())
						.issueType(issue.getIssuetype().getName()).parentKey(getParentOrNull(issue)).components(
								issue.getComponents().stream().map(JiraComponent::getName).collect(Collectors.toList()))
						.startDate(issue.getCreated()).endDate(issue.getUpdated()).build())
				.collect(Collectors.toList());
	}

	private static String getParentOrNull(Issue issue) {
		return Optional.ofNullable(issue.getParent()).map(Issue::getKey).orElse(null);
	}
}
