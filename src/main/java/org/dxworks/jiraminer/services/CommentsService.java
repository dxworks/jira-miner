package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.CommentsSearchResult;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;

import java.util.List;

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
        HttpResponse httpResponse = httpClient.get(new GenericUrl(apiPath));

        return httpResponse.parseAs(CommentsSearchResult.class).getComments();
    }

    public void addCommentsToIssue(Issue issue) {
        issue.setComments(getComments(issue.getKey()));
    }
}
