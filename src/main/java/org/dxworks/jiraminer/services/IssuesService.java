package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.request.issues.JiraIssuesRequestBody;
import org.dxworks.jiraminer.dto.response.issues.*;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLogsResponse;
import org.dxworks.jiraminer.pagination.IssueChangelogUrl;
import org.dxworks.utils.java.rest.client.response.HttpResponse;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
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

        int maxResults = 100;

        String jqlQuery = createJqlQuery(updatedAfter, updatedBefore, projectKeys);

        IssueSearchResult searchResult = searchIssues(apiPath, jqlQuery, maxResults, 0);

        int total = searchResult.getTotal();

        AtomicInteger progress = new AtomicInteger(1);

        int times = total / maxResults;
        int[] pages = IntStream.range(1, times).map(i -> i * maxResults).toArray();

        Stream<IssueSearchResult> allResults = getIssueSearchResult(apiPath, maxResults, jqlQuery, progress, times, pages);

        return Stream.concat(Stream.of(searchResult), allResults)
                .map(IssueSearchResult::getIssues)
                .flatMap(List::stream)
                .peek(this::addChangeLog)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    @NotNull
    private Stream<IssueSearchResult> getIssueSearchResult(String apiPath, int maxResults, String jqlQuery, AtomicInteger progress, int times, int[] pages) {
        if (pages.length == 0)
            return Stream.empty();

        Map<Boolean, List<IssueSearchResult>> results = IntStream.of(pages).parallel()
                .mapToObj(startAt -> getIssues(apiPath, maxResults, jqlQuery, progress, times, startAt))
                .collect(Collectors.groupingBy(result -> result instanceof IssueSearchResultWithErrors));

        int[] pagesWithErrors = results.getOrDefault((Boolean.TRUE), emptyList()).stream()
                .map(IssueSearchResultWithErrors.class::cast)
                .filter(response -> response.getHttpResponse().getStatusCode() == 429)
                .mapToInt(IssueSearchResultWithErrors::getStartAt).toArray();

        if (pagesWithErrors.length != 0) {
            log.warn("Waiting for 5s to request another {} pages", pagesWithErrors.length);
            Thread.sleep(5000);
        }

        return Stream.concat(results.getOrDefault(Boolean.FALSE, emptyList()).stream(), getIssueSearchResult(apiPath, maxResults, jqlQuery, progress, times, pagesWithErrors));
    }

    private IssueSearchResult getIssues(String apiPath, int maxResults, String jqlQuery, AtomicInteger progress, int times, int startAt) {
        IssueSearchResult issues = searchIssues(apiPath, jqlQuery, maxResults, startAt);
        if (!(issues instanceof IssueSearchResultWithErrors))
            log.info("Completed Step {} / {}", progress.getAndIncrement(), times);
        return issues;
    }

    private void addChangeLog(Issue issue) {
        if (issue.getChangelog().getMaxResults() < issue.getChangelog().getTotal()) {
            issue.getChangelog().getChanges().addAll(getChangeLogForIssue(issue.getKey(), issue.getChangelog().getMaxResults()));
        }
    }

    @SneakyThrows
    private IssueSearchResult searchIssues(String apiPath, String jqlQuery, int maxResults, int startAt) {
        JiraIssuesRequestBody jiraIssuesRequestBody = new JiraIssuesRequestBody(jqlQuery, startAt, maxResults, singletonList("changelog"));
        HttpResponse httpResponse = getHttpClient().post(new GenericUrl(apiPath),
                jiraIssuesRequestBody, null);
        try {
            if (!httpResponse.isSuccessStatusCode()) {
                log.warn("Failed Request: {} {} for {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl(), jiraIssuesRequestBody);
                httpResponse.parseAsString();
                return new IssueSearchResultWithErrors(httpResponse, startAt);
            }
            return parseIfOk(httpResponse, IssueSearchResult.class).orElseGet(IssueSearchResult::new);
        } catch (Exception e) {
            log.error("Search issues failed", e);
            return new IssueSearchResultWithErrors();
        }
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
