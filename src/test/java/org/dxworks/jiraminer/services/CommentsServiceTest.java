package org.dxworks.jiraminer.services;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.jiraminer.ServerInfoTestBase;
import org.dxworks.jiraminer.deployment.DeploymentType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueComment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class CommentsServiceTest extends ServerInfoTestBase {

    private static final HttpRequestInitializer NOOP = request -> {};
    private static final String ISSUE_WITH_COMMENTS = "TEST-1";
    private static final String ISSUE_WITHOUT_COMMENTS = "TEST-2";
    private static final String ISSUE_WITH_EMPTY_COMMENTS = "TEST-3";
    private static final String ISSUE_WITH_FAILED_FETCH = "TEST-4";
    private static final String CLOUD_ISSUE_WITH_COMMENTS = "CLOUD-1";
    private static final String COMMENT_ID = "comment-123";
    private static final String COMMENT_BODY = "Test comment";
    private static final String CLOUD_COMMENT_ID = "10033";
    private static final String CLOUD_COMMENT_BODY = "Why is this here?";

    private final List<String> requestedIssueKeys = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() throws IOException {
        startServerWithServerInfoHandler(exchange -> respondWithDeploymentType(exchange, "Server"));
        requestedIssueKeys.clear();
        // Handler for issue without comments - should return comments when fetched
        server.createContext("/rest/api/2/issue/" + ISSUE_WITHOUT_COMMENTS + "/comment", exchange -> {
            requestedIssueKeys.add(ISSUE_WITHOUT_COMMENTS);
            byte[] response = ("{\"comments\":[{\"id\":\"" + COMMENT_ID + "\",\"body\":\"" + COMMENT_BODY + "\"}]}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (java.io.OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        // Handler for issue with comments - should NEVER be called (test verifies skipping)
        server.createContext("/rest/api/2/issue/" + ISSUE_WITH_COMMENTS + "/comment", exchange -> {
            requestedIssueKeys.add(ISSUE_WITH_COMMENTS);
            fail("Should not fetch comments for issue that already has comments");
        });
        server.createContext("/rest/api/2/issue/" + ISSUE_WITH_EMPTY_COMMENTS + "/comment", exchange -> {
            requestedIssueKeys.add(ISSUE_WITH_EMPTY_COMMENTS);
            fail("Should not fetch comments for issue that already has an empty comments list");
        });
        server.createContext("/rest/api/2/issue/" + ISSUE_WITH_FAILED_FETCH + "/comment", exchange -> {
            requestedIssueKeys.add(ISSUE_WITH_FAILED_FETCH);
            byte[] response = "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, response.length);
            try (java.io.OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });

        server.createContext("/rest/api/3/issue/" + CLOUD_ISSUE_WITH_COMMENTS + "/comment", exchange -> {
            byte[] response = ("{\"comments\":[{\"id\":\"" + CLOUD_COMMENT_ID + "\",\"body\":{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"" + CLOUD_COMMENT_BODY + "\"}]}]}}]}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (java.io.OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
    }

    @Test
    void addCommentsToIssuesShouldFetchWhenMissingAndSkipWhenPresent() {
        CommentsService service = new CommentsService(context());
        
        // Issue with existing comments - should be skipped
        Issue issueWithComments = new Issue();
        issueWithComments.setKey(ISSUE_WITH_COMMENTS);
        ArrayList<IssueComment> existingComments = new ArrayList<>();
        IssueComment existingComment = new IssueComment();
        existingComment.setId("existing-1");
        existingComments.add(existingComment);
        issueWithComments.setComments(existingComments);

        Issue issueWithEmptyComments = new Issue();
        issueWithEmptyComments.setKey(ISSUE_WITH_EMPTY_COMMENTS);
        issueWithEmptyComments.setComments(new ArrayList<>());

        // Issue without comments - should be fetched
        Issue issueWithoutComments = new Issue();
        issueWithoutComments.setKey(ISSUE_WITHOUT_COMMENTS);
        issueWithoutComments.setComments(null);

        List<Issue> issues = List.of(issueWithComments, issueWithEmptyComments, issueWithoutComments);
        service.addCommentsToIssues(issues);

        // Verify issue with comments was skipped (original comments preserved)
        assertNotNull(issueWithComments.getComments());
        assertEquals(1, issueWithComments.getComments().size());
        assertEquals("existing-1", issueWithComments.getComments().get(0).getId());

        assertNotNull(issueWithEmptyComments.getComments());
        assertTrue(issueWithEmptyComments.getComments().isEmpty());

        // Verify issue without comments was fetched (new comments set)
        assertNotNull(issueWithoutComments.getComments());
        assertEquals(1, issueWithoutComments.getComments().size());
        assertEquals(COMMENT_ID, issueWithoutComments.getComments().get(0).getId());
        assertEquals(COMMENT_BODY, issueWithoutComments.getComments().get(0).getBody());
        assertEquals(List.of(ISSUE_WITHOUT_COMMENTS), requestedIssueKeys);
    }

    @Test
    void failedCommentFetchLeavesCommentsUnsetWhileOtherIssuesStillGetComments() {
        CommentsService service = new CommentsService(context());

        Issue issueWithFailedFetch = new Issue();
        issueWithFailedFetch.setKey(ISSUE_WITH_FAILED_FETCH);
        issueWithFailedFetch.setComments(null);

        Issue issueWithoutComments = new Issue();
        issueWithoutComments.setKey(ISSUE_WITHOUT_COMMENTS);
        issueWithoutComments.setComments(null);

        service.addCommentsToIssues(List.of(issueWithFailedFetch, issueWithoutComments));

        assertNull(issueWithFailedFetch.getComments());

        assertNotNull(issueWithoutComments.getComments());
        assertEquals(1, issueWithoutComments.getComments().size());
        assertEquals(COMMENT_ID, issueWithoutComments.getComments().get(0).getId());
        assertEquals(COMMENT_BODY, issueWithoutComments.getComments().get(0).getBody());

        assertEquals(2, requestedIssueKeys.size());
        assertTrue(requestedIssueKeys.containsAll(List.of(ISSUE_WITH_FAILED_FETCH, ISSUE_WITHOUT_COMMENTS)));
    }

    @Test
    void addCommentsToIssuesPropagatesRateLimitAbort() {
        IllegalStateException tenantQuotaAbort = new IllegalStateException("tenant quota exhausted");
        CommentsService service = new CommentsService(context()) {
            @Override
            public List<IssueComment> getComments(Issue issue) {
                throw tenantQuotaAbort;
            }
        };

        Issue issue = new Issue();
        issue.setKey(ISSUE_WITHOUT_COMMENTS);

        CompletionException thrown = assertThrows(CompletionException.class,
                () -> service.addCommentsToIssues(List.of(issue)));
        assertSame(tenantQuotaAbort, thrown.getCause());
        assertNull(issue.getComments());
    }

    @Test
    void getCommentsShouldExtractBodyFromCloudAdfFormat() {
        CommentsService service = new CommentsService(cloudContext());

        List<IssueComment> comments = service.getComments(CLOUD_ISSUE_WITH_COMMENTS);

        assertNotNull(comments);
        assertEquals(1, comments.size());
        assertEquals(CLOUD_COMMENT_ID, comments.get(0).getId());
        assertEquals(CLOUD_COMMENT_BODY, comments.get(0).getBody());
    }

    private JiraDeploymentContext context() {
        return new JiraDeploymentContext(jiraHome, NOOP, DeploymentType.Server);
    }

    private JiraDeploymentContext cloudContext() {
        return new JiraDeploymentContext(jiraHome, NOOP, DeploymentType.Cloud);
    }
}
