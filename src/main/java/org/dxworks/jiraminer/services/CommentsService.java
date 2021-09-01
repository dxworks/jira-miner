package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.CommentsSearchResult;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CommentsService extends JiraApiService {

    public CommentsService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public CommentsService(String jiraHome) {
        super(jiraHome);
    }

    public List<IssueComment> getComments(Issue issue) {
        return getComments(issue.getKey());
    }

    public List<IssueComment> getComments(String issueKey) {
        String apiPath = getApiPath("issue", issueKey, "comment");
        log.info("Getting comments for issue {}.", issueKey);
        HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);

        if (httpResponse.getStatusCode() == 429) {
            log.warn("Failed Request: {} {} for {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getRequest().getUrl());
            httpResponse.parseAsString();
            return null;
        }

        return parseIfOk(httpResponse, CommentsSearchResult.class)
            .map(CommentsSearchResult::getComments)
            .orElseGet(Collections::emptyList);
    }

    @SneakyThrows
    public void addCommentsToIssues(List<Issue> issues) {
        if(CollectionUtils.isEmpty(issues))
            return;

        Map<Issue, List<IssueComment>> issuesWithComments = issues.parallelStream()
            .collect(HashMap::new, (m, issue) -> m.put(issue, getComments(issue)), HashMap::putAll);

        issuesWithComments.entrySet().stream()
            .filter(e -> e.getValue() != null)
            .forEach(e -> e.getKey().setComments(e.getValue()));

        List<Issue> issuesWithFailedCommentRequests = issuesWithComments.entrySet().stream()
            .filter(e -> e.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        if(!issuesWithFailedCommentRequests.isEmpty()) {
            log.warn("Waiting for 5s to request comments for {} issues", issuesWithFailedCommentRequests.size());
            Thread.sleep(5000);
        }

        addCommentsToIssues(issuesWithFailedCommentRequests);
    }
}
