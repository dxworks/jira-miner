package org.dxworks.jiraminer.issues;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.ChangeItem;
import org.dxworks.jiraminer.dto.response.issues.ChangeLog;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IssuesServiceIT {

    private static final String JIRA_HOME = "https://loose.atlassian.net";

    private final IssuesService issuesService = new IssuesService(JIRA_HOME, TestUtils.getJiraAuthenticator());

    @Test
    void getAllIssuesForProjects() {
        List<Issue> issues = issuesService.getAllIssuesForProjects("SM");

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
        List<Issue> issues = issuesService.getAllIssuesForProjects("SM");

        issues.forEach(issue -> {
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChanges(issuesService.getChangeLogForIssue(issue.getKey()));
            issue.setChangelog(changeLog);
        });
        return issues;
    }
}
