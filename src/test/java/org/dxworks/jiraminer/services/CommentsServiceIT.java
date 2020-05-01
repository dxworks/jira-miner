package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.junit.jupiter.api.Test;

import java.util.List;

class CommentsServiceIT {
    public static final String ISSUE_KEY = "IG-11";
    private final CommentsService commentsService = new CommentsService(TestUtils.getJiraHome(),
            TestUtils.getJiraAuthenticator());

    @Test
    void getComments() {
        List<IssueComment> comments = commentsService.getComments(ISSUE_KEY);
        System.out.printf("Received %d comments", comments.size());
    }
}
