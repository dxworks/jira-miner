package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.configuration.ExportType;
import org.dxworks.jiraminer.deployment.DeploymentType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.request.issues.JiraCloudIssuesRequestBody;
import org.dxworks.jiraminer.dto.request.issues.JiraIssuesRequestBody;
import org.dxworks.jiraminer.dto.response.issues.*;
import org.dxworks.jiraminer.dto.response.issues.CloudIssueSearchResult;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLogsResponse;
import org.dxworks.jiraminer.pagination.IssueChangelogUrl;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;

@Slf4j
public class IssuesService extends JiraApiService {

    private final DeploymentType deploymentType;
    private final ExportType exportType;

    public IssuesService(JiraDeploymentContext context, ExportType exportType) {
        super(context.getJiraHome(), context.getApiVersion(), context.getRequestInitializer());
        this.deploymentType = context.getDeploymentType();
        this.exportType = exportType;
    }

    private boolean isDetailedExport() {
        return exportType == ExportType.DETAILED;
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
            HttpResponse httpResponse = rlGet(new IssueChangelogUrl(apiPath, startAt, maxResults));
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

        HttpResponse httpResponse = rlGet(new GenericUrl(apiPath));
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
        if (deploymentType == DeploymentType.Cloud) {
            return getAllIssuesForProjectsCloud(updatedAfter, updatedBefore, projectKeys);
        }
        return getAllIssuesForProjectsServer(updatedAfter, updatedBefore, projectKeys);
    }

    private List<Issue> getAllIssuesForProjectsServer(LocalDate updatedAfter, LocalDate updatedBefore, String... projectKeys) {
        String apiPath = getApiPath("search");

        int maxResults = 100;

        String jqlQuery = createJqlQuery(updatedAfter, updatedBefore, projectKeys);

        IssueSearchResult firstPage = searchIssues(apiPath, jqlQuery, maxResults, 0);

        int total = firstPage.getTotal();
        int[] pages = remainingPageStartAts(total, maxResults);
        int times = pages.length;
        AtomicInteger progress = new AtomicInteger(1);

        List<IssueSearchResult> remainingPages = getRateLimitedExecutor().submitAll(
                pages,
                startAt -> {
                    IssueSearchResult page = searchIssues(apiPath, jqlQuery, maxResults, startAt);
                    log.info("Completed Step {} / {}", progress.getAndIncrement(), times);
                    return page;
                });

        List<Issue> all = new ArrayList<>();
        all.addAll(firstPage.getIssues());
        remainingPages.stream().map(IssueSearchResult::getIssues).forEach(all::addAll);
        return all;
    }

    private List<Issue> getAllIssuesForProjectsCloud(LocalDate updatedAfter, LocalDate updatedBefore, String... projectKeys) {
        String apiPath = getApiPath("search", "jql");
        int maxResults = 100;
        String jqlQuery = createJqlQuery(updatedAfter, updatedBefore, projectKeys);

        List<Issue> all = new ArrayList<>();
        String nextPageToken = null;
        int pageCount = 0;

        do {
            CloudIssueSearchResult page = searchIssuesCloud(apiPath, jqlQuery, maxResults, nextPageToken);
            all.addAll(page.getIssues());
            nextPageToken = page.getNextPageToken();
            pageCount++;
            log.info("Completed Cloud page {} with {} issues", pageCount, page.getIssues().size());
        } while (nextPageToken != null && !nextPageToken.isEmpty());

        return all;
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

    public void addChangeLog(Issue issue) {
        if (issue.getChangelog() == null) {
            // Changelog is missing entirely, fetch full changelog
            ChangeLog changeLog = new ChangeLog();
            changeLog.setChanges(getChangeLogForIssue(issue.getKey(), 0));
            changeLog.setMaxResults(changeLog.getChanges().size());
            changeLog.setTotal(changeLog.getChanges().size());
            issue.setChangelog(changeLog);
        } else {
            int loadedEntries = issue.getChangelog().getChanges().size();
            if (loadedEntries < issue.getChangelog().getTotal()) {
                // Changelog is paginated, fetch remaining pages
                issue.getChangelog().getChanges().addAll(getChangeLogForIssue(issue.getKey(), loadedEntries));
            }
        }
        // If changelog is already complete (changes.size >= total), no-op
    }

    private IssueSearchResult searchIssues(String apiPath, String jqlQuery, int maxResults, int startAt) {
        JiraIssuesRequestBody body = new JiraIssuesRequestBody(jqlQuery, startAt, maxResults, getExpandList());
        HttpResponse httpResponse = rlPost(new GenericUrl(apiPath), body);
        if (!httpResponse.isSuccessStatusCode()) {
            log.warn("Failed Request: {} {} for {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl(), body);
            httpResponse.parseAsString();
            throw new IllegalStateException(String.format(
                    "Search issues failed for startAt=%d with status=%d", startAt, httpResponse.getStatusCode()));
        }
        return parseIfOk(httpResponse, IssueSearchResult.class).orElseGet(IssueSearchResult::new);
    }

    private CloudIssueSearchResult searchIssuesCloud(String apiPath, String jqlQuery, int maxResults, String nextPageToken) {
        JiraCloudIssuesRequestBody body = new JiraCloudIssuesRequestBody();
        body.setJql(jqlQuery);
        body.setMaxResults(maxResults);
        body.setFields(singletonList("*all"));
        body.setNextPageToken(nextPageToken);
        body.setExpand(getCloudExpand());

        HttpResponse httpResponse = rlPost(new GenericUrl(apiPath), body);
        if (!httpResponse.isSuccessStatusCode()) {
            log.warn("Failed Request: {} {} for {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl(), body);
            httpResponse.parseAsString();
            throw new IllegalStateException(String.format(
                    "Search issues failed for nextPageToken=%s with status=%d", nextPageToken, httpResponse.getStatusCode()));
        }
        return parseIfOk(httpResponse, CloudIssueSearchResult.class).orElseGet(CloudIssueSearchResult::new);
    }

    private List<String> getExpandList() {
        return isDetailedExport() ? singletonList("changelog") : Collections.emptyList();
    }

    private String getCloudExpand() {
        return getExpandList().isEmpty() ? null : String.join(",", getExpandList());
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
