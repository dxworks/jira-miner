package org.dxworks.jiraminer.main;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.configuration.JiraMinerConfiguration;
import org.dxworks.jiraminer.configuration.JiraMinerConfigurer;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.JiraComponent;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.export.ResultExporter;
import org.dxworks.jiraminer.issues.CommentsService;
import org.dxworks.utils.java.rest.client.utils.JsonMapper;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@Slf4j
public class Main {

	public static final String BASIC = "basic";
	public static final String DETAILED = "detailed";
	private static JiraMinerConfigurer jiraMinerConfigurer;

	public static void main(String[] args) {
		log.info("Starting Jira Miner...");

		JiraMinerConfiguration jiraMinerConfiguration = JiraMinerConfiguration.getInstance();

		List<Issue> issues = null;
		try {
			jiraMinerConfigurer = new JiraMinerConfigurer(jiraMinerConfiguration);
			issues = jiraMinerConfigurer.configureIssuesService()
					.getAllIssuesForProjects(jiraMinerConfiguration.getProjects());
		} catch (Exception e) {
			log.error("Error retrieving issues. Please revise your authentication method and try again. \n"
					+ "If you are using cookie based authentication, please renew the cookie!", e);
			System.exit(1);
		}

		log.info("Writing results to file...");
		ensureResultsFolderExists();
		List<String> exportTypes = Optional.ofNullable(jiraMinerConfiguration.getProperty("exportTypes"))
				.map(types -> asList(types.split(","))).orElse(singletonList(BASIC));
		String projectID = jiraMinerConfiguration.getProjectID();
		if (exportTypes.contains(BASIC)) {
			writeBasicIssuesToFile(projectID, issues);
		}
		if (exportTypes.contains(DETAILED)) {
			CommentsService commentsService = jiraMinerConfigurer.configureCommentsService();
			issues.forEach(commentsService::addCommentsToIssue);
			List<IssueStatus> allStatuses = jiraMinerConfigurer.configureStatusesService().getAllStatuses();
			new ResultExporter().export(issues, allStatuses, getOutputFIle(projectID + "-detailed"));
		}
		log.info("Finished Jira Miner.");
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