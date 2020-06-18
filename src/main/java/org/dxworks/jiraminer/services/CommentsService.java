package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.CommentsSearchResult;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

@Slf4j
public class CommentsService extends JiraApiService {

    public CommentsService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public CommentsService(String jiraHome) {
        super(jiraHome);
    }

    @SneakyThrows
    public List<IssueComment> getComments(String issueKey) {
        String apiPath = getApiPath("issue", issueKey, "comment");
        log.info("Getting comments for issue {}.", issueKey);
        HttpResponse httpResponse = httpClient.get(new GenericUrl(apiPath));

        return httpResponse.parseAs(CommentsSearchResult.class).getComments();
    }

    public void addCommentsToIssue(Issue issue) {
        issue.setComments(getComments(issue.getKey()));
    }
}
