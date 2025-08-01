package org.dxworks.jiraminer.main;

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
import java.util.*;
import java.util.stream.Collectors;

import static org.dxworks.jiraminer.cache.CacheRepository.merge;

@Slf4j
public class Main {

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
            log.error("Error getting issues", e);
		}

		log.info("Writing results to file...");
		ensureResultsFolderExists();
		String projectID = jiraMinerConfiguration.getProjectId();
			writeBasicIssuesToFile(projectID, issuesAndStatuses.left);

		if (jiraMinerConfiguration.needsDetailedExport()) {
			new ResultExporter().export(issuesAndStatuses.left, issuesAndStatuses.right, getOutputFIle(projectID + "-detailed"));
		}
		log.info("Finished Jira Miner.");
	}

	private static ImmutablePair<List<Issue>, List<IssueStatus>> getIssuesAndStatusesCaching(JiraMinerConfiguration jiraMinerConfiguration) {
		IssuesService issuesService = jiraMinerConfigurer.configureIssuesService();
		CommentsService commentsService = jiraMinerConfigurer.configureCommentsService();
		StatusesService statusesService = jiraMinerConfigurer.configureStatusesService();
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
						.startDate(issue.getCreated()).updatedDate(issue.getUpdated())
						.resolutionDate(issue.getResolutiondate())
						.dueDate(issue.getFields().getDuedate())
						.environment(issue.getFields().getEnvironment())
						.resolution(issue.getFields().getResolution() != null && issue.getFields().getResolution().get("name") != null ? 
							issue.getFields().getResolution().get("name").toString() : null)
						.labels(issue.getFields().getLabels())
						.fixVersions(extractVersionNames(issue.getFields().getFixVersions()))
						.affectsVersions(extractVersionNames(issue.getFields().getVersions()))
						.workRatio(issue.getFields().getWorkratio())
						.issueLinks(extractIssueLinks(issue.getFields().getIssuelinks()))
						.build())
				.collect(Collectors.toList());
	}
	
	private static List<Map<String, String>> extractIssueLinks(List<Map<String, Object>> issueLinks) {
		if (issueLinks == null) {
			return null;
		}
		return issueLinks.stream()
			.filter(Objects::nonNull)
			.map(link -> {
				Map<String, String> linkInfo = new HashMap<>();
				
				Object issueLinkType = link.get("type");
				if (issueLinkType instanceof Map) {
					Map<?, ?> linkType = (Map<?, ?>)issueLinkType;
					Object name = linkType.get("name");
					Object inward = linkType.get("inward");
					Object outward = linkType.get("outward");
					
					if (name != null) {
						linkInfo.put("type", name.toString());
					}
					
					extractLinkedIssueInfo(link, linkInfo, "outwardIssue", "outward", outward);
					
					if (!linkInfo.containsKey("key")) {
						extractLinkedIssueInfo(link, linkInfo, "inwardIssue", "inward", inward);
					}
				}
				
				return linkInfo.isEmpty() ? null : linkInfo;
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
	
	private static void extractLinkedIssueInfo(Map<String, Object> link, Map<String, String> linkInfo, 
			String issueType, String directionType, Object directionDescription) {
		Object issue = link.get(issueType);
		if (issue instanceof Map) {
			Object key = ((Map<?, ?>)issue).get("key");
			if (key != null) {
				linkInfo.put("key", key.toString());
				linkInfo.put("direction", directionType);
				if (directionDescription != null) {
					linkInfo.put("description", directionDescription.toString());
				}
			}
		}
	}
	
	private static List<String> extractVersionNames(List<Map<String, Object>> versions) {
		if (versions == null) {
			return null;
		}
		return versions.stream()
			.filter(Objects::nonNull)
			.map(v -> v.get("name"))
			.filter(Objects::nonNull)
			.map(Object::toString)
			.collect(Collectors.toList());
	}

	private static String getParentOrNull(Issue issue) {
		return Optional.ofNullable(issue.getParent()).map(Issue::getKey).orElse(null);
	}
}
