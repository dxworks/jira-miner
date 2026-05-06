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

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

@Slf4j
public class IssuesService extends JiraApiService {
    private static final int MAX_RETRY_ATTEMPTS = 6;
    private static final long INITIAL_BACKOFF_MILLIS = 5000;
    private static final long MAX_BACKOFF_MILLIS = 60000;


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
        String apiPath = getApiPath(ImmutableMap.of("issueId", issueKey), "issue", ":issueId", "worklog");

        List<WorkLog> allWorkLogs = new ArrayList<>();

        HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);
        Optional<WorkLogsResponse> workLogsResponse = parseIfOk(httpResponse, WorkLogsResponse.class);
        workLogsResponse.map(WorkLogsResponse::getWorklogs).ifPresent(allWorkLogs::addAll);

        log.info("Got {} work logs for issue {}", allWorkLogs.size(), issueKey);

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

        IssueSearchResult searchResult = getFirstPageWithRetry(apiPath, jqlQuery, maxResults);

        int total = searchResult.getTotal();

        AtomicInteger progress = new AtomicInteger(1);

        int[] pages = remainingPageStartAts(total, maxResults);
        int times = pages.length;

        Stream<IssueSearchResult> allResults = getIssueSearchResult(apiPath, maxResults, jqlQuery, progress, times, pages);

        return Stream.concat(Stream.of(searchResult), allResults)
                .map(IssueSearchResult::getIssues)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static int[] remainingPageStartAts(int total, int maxResults) {
        int pageCount = pageCount(total, maxResults);
        return IntStream.range(1, pageCount).map(i -> i * maxResults).toArray();
    }

    private static int pageCount(int total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        }

        return (total + pageSize - 1) / pageSize;
    }

    @SneakyThrows
    @NotNull
    private Stream<IssueSearchResult> getIssueSearchResult(String apiPath, int maxResults, String jqlQuery, AtomicInteger progress, int times, int[] pages) {
        if (pages.length == 0)
            return Stream.empty();

        return IntStream.of(pages)
                .parallel()
                .mapToObj(startAt -> getIssues(apiPath, maxResults, jqlQuery, progress, times, startAt));
    }

    private IssueSearchResult getFirstPageWithRetry(String apiPath, String jqlQuery, int maxResults) {
        return getIssuesWithRetry(apiPath, maxResults, jqlQuery, 0);
    }

    private long backoffMillis(int attempt) {
        long exponential = INITIAL_BACKOFF_MILLIS * (1L << Math.max(0, attempt - 1));
        return Math.min(exponential, MAX_BACKOFF_MILLIS);
    }

    private IssueSearchResult getIssues(String apiPath, int maxResults, String jqlQuery, AtomicInteger progress, int times, int startAt) {
        IssueSearchResult issues = getIssuesWithRetry(apiPath, maxResults, jqlQuery, startAt);
        log.info("Completed Step {} / {}", progress.getAndIncrement(), times);
        return issues;
    }

    @SneakyThrows
    private IssueSearchResult getIssuesWithRetry(String apiPath, int maxResults, String jqlQuery, int startAt) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            IssueSearchResult result = searchIssues(apiPath, jqlQuery, maxResults, startAt);
            if (!(result instanceof IssueSearchResultWithErrors)) {
                return result;
            }

            IssueSearchResultWithErrors error = (IssueSearchResultWithErrors) result;
            HttpResponse response = error.getHttpResponse();
            boolean timedOut = response == null;
            boolean rateLimited = response != null && response.getStatusCode() == 429;

            if (!timedOut && !rateLimited) {
                throw new IllegalStateException(String.format(
                        "Non-retriable issue page failure for startAt=%d, status=%d.",
                        startAt,
                        response.getStatusCode()
                ));
            }

            if (attempt == MAX_RETRY_ATTEMPTS) {
                throw new IllegalStateException(String.format(
                        "Retry limit reached for issue page startAt=%d after %d attempts (%s)",
                        startAt,
                        attempt,
                        timedOut ? "timeout" : "429"
                ));
            }

            long backoffMillis = backoffMillis(attempt);
            log.warn("Retrying issue page startAt={} in {} ms after {} (attempt {}/{})",
                    startAt,
                    backoffMillis,
                    timedOut ? "timeout" : "429",
                    attempt + 1,
                    MAX_RETRY_ATTEMPTS);
            Thread.sleep(backoffMillis);
        }

        throw new IllegalStateException(String.format("Could not fetch issue page startAt=%d", startAt));
    }

    public void addChangeLog(Issue issue) {
        if (issue.getChangelog().getMaxResults() < issue.getChangelog().getTotal()) {
            issue.getChangelog().getChanges().addAll(getChangeLogForIssue(issue.getKey(), issue.getChangelog().getMaxResults()));
        }
    }

    private IssueSearchResult searchIssues(String apiPath, String jqlQuery, int maxResults, int startAt) {
        JiraIssuesRequestBody jiraIssuesRequestBody = new JiraIssuesRequestBody(jqlQuery, startAt, maxResults, singletonList("changelog"));
        try {
            HttpResponse httpResponse = getHttpClient().post(new GenericUrl(apiPath),
                    jiraIssuesRequestBody, null);
            if (!httpResponse.isSuccessStatusCode()) {
                log.warn("Failed Request: {} {} for {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl(), jiraIssuesRequestBody);
                httpResponse.parseAsString();
                return new IssueSearchResultWithErrors(httpResponse, startAt);
            }
            return parseIfOk(httpResponse, IssueSearchResult.class).orElseGet(IssueSearchResult::new);
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.warn("Search issues timed out for startAt {}", startAt);
                return new IssueSearchResultWithErrors(null, startAt);
            }

            throw new IllegalStateException(String.format("Search issues failed for startAt %d", startAt), e);
        }
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
