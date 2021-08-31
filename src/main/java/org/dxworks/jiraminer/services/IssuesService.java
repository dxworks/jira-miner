package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.request.issues.JiraIssuesRequestBody;
import org.dxworks.jiraminer.dto.response.issues.ChangeLog;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueChange;
import org.dxworks.jiraminer.dto.response.issues.IssueSearchResult;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLogsResponse;
import org.dxworks.jiraminer.pagination.IssueChangelogUrl;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Slf4j
public class IssuesService extends JiraApiService {


    public IssuesService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public IssuesService(String jiraHome) {
        super(jiraHome);
    }

    @SneakyThrows
    public List<IssueChange> getChangeLogForIssue(String issueId) {
        return getChangeLogForIssue(issueId, 0);
    }

    @SneakyThrows
    public List<IssueChange> getChangeLogForIssue(String issueKey, int startAt) {
        String apiPath = getApiPath(ImmutableMap.of("issueId", issueKey), "issue", ":issueId");

        List<IssueChange> allChanges = new ArrayList<>();
        int maxResults = 100;
        int total;

        do {
            HttpResponse httpResponse = getHttpClient().get(new IssueChangelogUrl(apiPath, startAt, maxResults), null);
            Optional<Issue> issue = parseIfOk(httpResponse, Issue.class);
            issue.map(Issue::getChangelog).map(ChangeLog::getChanges).ifPresent(allChanges::addAll);

            total = issue.map(Issue::getChangelog).map(ChangeLog::getTotal).orElse(0);
            startAt = startAt + maxResults;
            log.info("Got changes for issue {} ({}/{})", issueKey, Math.min(startAt, total), total);
        } while (startAt < total);

        return allChanges;
    }

    public List<WorkLog> getWorkLogsForIssue(String issueKey) {
        return getWorkLogsForIssue(issueKey, 0);
    }

    public List<WorkLog> getWorkLogsForIssue(String issueKey, int startAt) {
        String apiPath = getApiPath(ImmutableMap.of("issueId", issueKey), "issue", ":issueId", "worklog");

        List<WorkLog> allWorkLogs = new ArrayList<>();
        int maxResults = 100;
        int total;

        do {
            HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);
            Optional<WorkLogsResponse> workLogsResponse = parseIfOk(httpResponse, WorkLogsResponse.class);
            workLogsResponse.map(WorkLogsResponse::getWorklogs).ifPresent(allWorkLogs::addAll);

            total = workLogsResponse.map(WorkLogsResponse::getTotal).orElse(0);
            startAt = startAt + maxResults;
            log.info("Got work logs for issue {} ({}/{})", issueKey, Math.min(startAt, total), total);
        } while (startAt < total);

        return allWorkLogs;
    }

    public List<Issue> getAllIssuesForProjects(List<String> projectKeys) {
        return getAllIssuesForProjects(null, null, projectKeys);
    }

    public List<Issue> getAllIssuesForProjects(LocalDate updatedAfter, LocalDate updatedBefore, List<String> projectKeys) {
        return getAllIssuesForProjects(updatedAfter, updatedBefore, projectKeys.toArray(new String[0]));
    }

    public List<Issue> getAllIssuesForProjects(String... projectKeys) {
        return getAllIssuesForProjects(null, null, projectKeys);
    }

    public List<Issue> getAllIssuesForProjects(LocalDate updatedAfter, LocalDate updatedBefore, String... projectKeys) {
        String apiPath = getApiPath("search");

        List<Issue> allIssues = new ArrayList<>();

        int startAt = 0;
        int maxResults = 100;
        int total;

        String jqlQuery = createJqlQuery(updatedAfter, updatedBefore, projectKeys);
        do {
            IssueSearchResult searchResult = searchIssues(apiPath, jqlQuery, maxResults, startAt);

            allIssues.addAll(searchResult.getIssues());

            total = searchResult.getTotal();
            startAt = startAt + maxResults;
            log.info("Got issues ({}/{})", Math.min(startAt, total), total);
        } while (startAt < total);

        allIssues.forEach(issue -> {
            if (issue.getChangelog().getMaxResults() < issue.getChangelog().getTotal()) {
                issue.getChangelog().getChanges().addAll(getChangeLogForIssue(issue.getKey(), issue.getChangelog().getMaxResults()));
            }
        });

        return allIssues;
    }

    @SneakyThrows
    private IssueSearchResult searchIssues(String apiPath, String jqlQuery, int maxResults, int startAt) {
        HttpResponse httpResponse = getHttpClient().post(new GenericUrl(apiPath),
                new JiraIssuesRequestBody(jqlQuery, startAt, maxResults, singletonList("changelog")), null);
        return parseIfOk(httpResponse, IssueSearchResult.class).orElseGet(IssueSearchResult::new);
    }

    private String createJqlQuery(LocalDate updatedAfter, LocalDate updatedBefore, String... existingJiraProjects) {
        String jql = "project in (";
        jql += Arrays.stream(existingJiraProjects).map(this::encloseInQuotes).collect(Collectors.joining(","));
        jql += ")";
        jql += Optional.ofNullable(updatedAfter)
                .map(updated -> " and updated > " + updated.format(DateTimeFormatter.ofPattern("\"yyyy/MM/dd\"")))
                .orElse("");

        jql += Optional.ofNullable(updatedBefore)
                .map(updated -> " and updated < " + updated.format(DateTimeFormatter.ofPattern("\"yyyy/MM/dd\"")))
                .orElse("");

        return jql;
    }

    private String encloseInQuotes(String s) {
        return String.format("\"%s\"", s);
    }

}
