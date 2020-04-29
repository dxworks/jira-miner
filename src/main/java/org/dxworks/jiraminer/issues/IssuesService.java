package org.dxworks.jiraminer.issues;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.request.issues.JiraIssuesRequestBody;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueChange;
import org.dxworks.jiraminer.dto.response.issues.IssueSearchResult;
import org.dxworks.jiraminer.pagination.IssueChangelogUrl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            HttpResponse httpResponse = httpClient.get(new IssueChangelogUrl(apiPath, startAt, maxResults));

            Issue issue = httpResponse.parseAs(Issue.class);

            allChanges.addAll(issue.getChangelog().getChanges());

            total = issue.getChangelog().getTotal();
            startAt = startAt + maxResults;
            log.info("Got changes for issue {} ({}/{})", issueKey, Math.min(startAt, total), total);
        } while (startAt < total);

        return allChanges;
    }

    public List<Issue> getAllIssuesForProjects(List<String> projectKeys) {
        return getAllIssuesForProjects(projectKeys.toArray(new String[0]));
    }

    public List<Issue> getAllIssuesForProjects(String... projectKeys) {
        String apiPath = getApiPath("search");

        List<Issue> allIssues = new ArrayList<>();

        int startAt = 0;
        int maxResults = 500;
        int total;

        String jqlQuery = createJqlQuery(projectKeys);
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
        HttpResponse httpResponse = httpClient.post(new GenericUrl(apiPath),
                new JiraIssuesRequestBody(jqlQuery, startAt, maxResults, singletonList("changelog")));
        return httpResponse.parseAs(IssueSearchResult.class);
    }

    private String createJqlQuery(String... existingJiraProjects) {
        String jql = "project in (";
        jql += Arrays.stream(existingJiraProjects)
                .map(this::encloseInQuotes)
                .collect(Collectors.joining(","));
        jql += ")";

        return jql;
    }

    private String encloseInQuotes(String s) {
        return String.format("\"%s\"", s);
    }

}
