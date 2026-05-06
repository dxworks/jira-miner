package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.CommentsSearchResult;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Collections;
import java.util.List;

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
        HttpResponse httpResponse = rlGet(new GenericUrl(apiPath));
        return parseIfOk(httpResponse, CommentsSearchResult.class)
                .map(CommentsSearchResult::getComments)
                .orElseGet(Collections::emptyList);
    }

    /**
     * Fetch comments for every issue concurrently through the shared rate-limited
     * executor, then attach the result. Per-issue failures are logged and the issue
     * is left without comments rather than aborting the entire run.
     */
    public void addCommentsToIssues(List<Issue> issues) {
        if (CollectionUtils.isEmpty(issues)) {
            return;
        }

        List<List<IssueComment>> commentsPerIssue = getRateLimitedExecutor().submitAll(
                issues,
                issue -> {
                    try {
                        return getComments(issue);
                    } catch (Exception e) {
                        log.warn("Could not fetch comments for issue {}: {}", issue.getKey(), e.getMessage());
                        return null;
                    }
                });

        for (int i = 0; i < issues.size(); i++) {
            List<IssueComment> comments = commentsPerIssue.get(i);
            if (comments != null) {
                issues.get(i).setComments(comments);
            }
        }
    }
}
