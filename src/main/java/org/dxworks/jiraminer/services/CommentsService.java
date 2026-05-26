package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.CommentsSearchResult;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Collections;
import java.util.List;

@Slf4j
public class CommentsService extends JiraApiService {

    public CommentsService(JiraDeploymentContext context) {
        super(context.getJiraHome(), context.getApiVersion(), context.getRequestInitializer());
    }

    public List<IssueComment> getComments(Issue issue) {
        return getComments(issue.getKey());
    }

    public List<IssueComment> getComments(String issueKey) {
        String apiPath = getApiPath("issue", issueKey, "comment");
        log.info("Getting comments for issue {}.", issueKey);
        HttpResponse httpResponse = rlGet(new GenericUrl(apiPath));
        CommentsSearchResult commentsSearchResult = parseIfOk(httpResponse, CommentsSearchResult.class)
                .orElse(null);
        if (commentsSearchResult == null) {
            return null;
        }
        return commentsSearchResult.getComments() != null
                ? commentsSearchResult.getComments()
                : Collections.emptyList();
    }

    /**
     * Fetch comments for issues that don't already have them, concurrently through the
     * shared rate-limited executor. Per-issue failures are logged and the issue is left
     * without comments rather than aborting the entire run.
     */
    public void addCommentsToIssues(List<Issue> issues) {
        if (CollectionUtils.isEmpty(issues)) {
            return;
        }

        // Filter to only issues without comments (null = not attempted, non-null = attempted even if empty)
        List<Issue> issuesNeedingComments = issues.stream()
                .filter(issue -> issue.getComments() == null)
                .toList();

        if (issuesNeedingComments.isEmpty()) {
            log.info("All issues already have comments (including those with 0 comments), skipping comment fetch");
            return;
        }

        log.info("Fetching comments for {} issues (skipping {} with comments)", 
                issuesNeedingComments.size(), issues.size() - issuesNeedingComments.size());

        List<List<IssueComment>> commentsPerIssue = getRateLimitedExecutor().submitAll(
                issuesNeedingComments,
                issue -> {
                    try {
                        return getComments(issue);
                    } catch (Exception e) {
                        log.warn("Could not fetch comments for issue {}: {}", issue.getKey(), e.getMessage());
                        return null;
                    }
                });

        for (int i = 0; i < issuesNeedingComments.size(); i++) {
            List<IssueComment> comments = commentsPerIssue.get(i);
            if (comments != null) {
                issuesNeedingComments.get(i).setComments(comments);
            }
        }
    }
}
