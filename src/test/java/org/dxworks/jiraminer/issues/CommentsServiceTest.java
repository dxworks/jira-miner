package org.dxworks.jiraminer.issues;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.junit.jupiter.api.Test;

import java.util.List;

class CommentsServiceTest {
    public static final String ISSUE_KEY = "IG-11";
    private static final String JIRA_HOME = "https://inspectorgit.atlassian.net";
    private final CommentsService commentsService = new CommentsService(JIRA_HOME,
            TestUtils.getJiraAuthenticator());

    @Test
    void getComments() {
        List<IssueComment> comments = commentsService.getComments(ISSUE_KEY);
        System.out.printf("Received %d comments", comments.size());
    }
}
