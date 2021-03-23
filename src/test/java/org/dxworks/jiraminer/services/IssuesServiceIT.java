package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.ChangeItem;
import org.dxworks.jiraminer.dto.response.issues.ChangeLog;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueChange;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class IssuesServiceIT {

	private static final String JIRA_HOME = TestUtils.getJiraHome();
	public static final String PROJECT_KEY = "IG";

	private final IssuesService issuesService = new IssuesService(JIRA_HOME, TestUtils.getJiraAuthenticator());

	@Test
	void getAllIssuesForProjects() {
		List<Issue> issues = issuesService.getAllIssuesForProjects(PROJECT_KEY);

		assertTrue(issues.size() >= 25);


		Map<String, Issue> keyToIssue = issues.stream().collect(Collectors.toMap(Issue::getKey, Function.identity()));

        double averageSubtasksPerStory = issues.stream()
                .filter(issue -> !issue.getIssuetype().isSubTask())
                .sorted(Comparator.comparing(issue -> issue.getSubtasks().size()))
                .peek(issue -> System.out.println(String.format("[%d] - [%s]: %s %s", issue.getSubtasks().size(), issue.getIssuetype().getName().toUpperCase().charAt(0), issue.getKey(), issue.getSummary())))
                .mapToInt(issue -> issue.getSubtasks().size())
                .average().getAsDouble();

        System.out.printf("Average subtasks per story: %f", averageSubtasksPerStory);

    }

    @Test
    void testGetChangesForAllIssues() {
		List<Issue> issues = getIssuesWithChanges();

		issues.forEach(issue -> {
			System.out.println(String.format("%s: %s", issue.getKey(), issue.getSummary()));
			issue.getChangelog().getChanges().stream().filter(issueChange -> issueChange.getItems().stream()
					.anyMatch(changeItem -> "status".equalsIgnoreCase(changeItem.getField()))).forEach(issueChange -> {
				Optional<ChangeItem> first = issueChange.getItems().stream()
						.filter(changeItem -> "status".equalsIgnoreCase(changeItem.getField())).findFirst();
				first.ifPresent(changeItem -> System.out.println(
						String.format("\t%s: [%s] -> [%s]", issueChange.getCreated(), changeItem.getFromString(),
								changeItem.getToString())));
			});
		});
	}

	private List<Issue> getIssuesWithChanges() {
		List<Issue> issues = issuesService.getAllIssuesForProjects(PROJECT_KEY);

		issues.forEach(issue -> {
			ChangeLog changeLog = new ChangeLog();
			changeLog.setChanges(issuesService.getChangeLogForIssue(issue.getKey()));
			issue.setChangelog(changeLog);
		});
		return issues;
	}

	@Test
	void testGetAllIssuesForProjectsAfter() {
		List<Issue> issues = issuesService.getAllIssuesForProjects(LocalDate.now().minusDays(7), null, PROJECT_KEY);
		assertNotNull(issues);
	}

	@Test
	void testGetAllIssuesForProjectsBefore() {
		List<Issue> issues = issuesService.getAllIssuesForProjects(null, LocalDate.now().minusDays(7), PROJECT_KEY);
		assertNotNull(issues);
	}

	@Test
	void testGetAllIssuesForProjectsBetween() {
		List<Issue> issues = issuesService.getAllIssuesForProjects(LocalDate.now().minusDays(7), LocalDate.now().minusDays(2), PROJECT_KEY);
		assertNotNull(issues);
	}

	@Test
	void testGetWorkLogsForIssue() {
		List<WorkLog> workLogs = issuesService.getAllIssuesForProjects(PROJECT_KEY).stream()
				.flatMap(issue -> issuesService.getWorkLogsForIssue(issue.getKey()).stream())
				.collect(Collectors.toList());
		assertFalse(workLogs.isEmpty());

	}
}
