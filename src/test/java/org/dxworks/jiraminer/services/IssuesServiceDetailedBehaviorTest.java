package org.dxworks.jiraminer.services;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.jiraminer.ServerInfoTestBase;
import org.dxworks.jiraminer.configuration.ExportType;
import org.dxworks.jiraminer.deployment.DeploymentType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.ChangeLog;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IssuesServiceDetailedBehaviorTest extends ServerInfoTestBase {

    private static final HttpRequestInitializer NOOP = request -> {};
    private static final String TEST_ISSUE_KEY = "TEST-1";
    private static final String CHANGELOG_ENDPOINT = "/rest/api/2/issue/" + TEST_ISSUE_KEY;
    private static final int TOTAL_CHANGELOG_ENTRIES = 100;
    private static final int PARTIAL_CHANGELOG_ENTRIES = 50;
    private static final String FIRST_CHANGE_ID = "change-0";
    private static final String LAST_CHANGE_ID = "change-99";
    
    private boolean handlerCalled = false;
    private final List<Integer> requestedStartIndexes = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        startServerWithServerInfoHandler(exchange -> respondWithDeploymentType(exchange, "Server"));
        createChangelogEndpointHandler();
    }

    private void createChangelogEndpointHandler() {
        handlerCalled = false;
        requestedStartIndexes.clear();
        allowHandlerCalls = true;
        server.createContext(CHANGELOG_ENDPOINT, exchange -> {
            if (!allowHandlerCalls) {
                fail("Should not fetch changelog when already complete");
            }
            handlerCalled = true;
            int startIndex = parseStartIndex(exchange);
            requestedStartIndexes.add(startIndex);
            int remainingEntries = Math.max(0, TOTAL_CHANGELOG_ENTRIES - startIndex);
            byte[] response = buildChangelogResponse(startIndex, remainingEntries).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sendJsonResponse(exchange, response);
        });
    }
    
    private boolean allowHandlerCalls = true;

    private int parseStartIndex(com.sun.net.httpserver.HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isEmpty()) {
            return 0;
        }

        for (String parameter : query.split("&")) {
            if (parameter.startsWith("startIndex=")) {
                return Integer.parseInt(parameter.substring("startIndex=".length()));
            }
        }

        return 0;
    }

    private String buildChangelogJson(int startIndex, int count) {
        StringBuilder changes = new StringBuilder("[");
        for (int i = startIndex; i < startIndex + count; i++) {
            if (i > startIndex) changes.append(",");
            changes.append("{\"id\":\"change-" + i + "\"}");
        }
        changes.append("]");
        return changes.toString();
    }

    private String buildChangelogResponse(int startIndex, int count) {
        return "{\"key\":\"" + TEST_ISSUE_KEY + "\",\"fields\":{},\"changelog\":{\"maxResults\":" + count + ",\"total\":" + TOTAL_CHANGELOG_ENTRIES + ",\"histories\":" + buildChangelogJson(startIndex, count) + "}}";
    }

    private void sendJsonResponse(com.sun.net.httpserver.HttpExchange exchange, byte[] response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (java.io.OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    @Test
    void addChangeLogShouldFetchWhenMissing() {
        IssuesService service = new IssuesService(context(DeploymentType.Server), ExportType.DETAILED);
        Issue issue = new Issue();
        issue.setKey(TEST_ISSUE_KEY);
        issue.setChangelog(null);

        service.addChangeLog(issue);

        // Verify handler was called
        assertTrue(handlerCalled, "Handler should be called when changelog is missing");
        assertEquals(List.of(0), requestedStartIndexes);
        
        // Verify specific data was fetched
        assertNotNull(issue.getChangelog());
        assertEquals(TOTAL_CHANGELOG_ENTRIES, issue.getChangelog().getChanges().size());
        assertEquals(TOTAL_CHANGELOG_ENTRIES, issue.getChangelog().getMaxResults());
        assertEquals(TOTAL_CHANGELOG_ENTRIES, issue.getChangelog().getTotal());
        assertEquals(FIRST_CHANGE_ID, issue.getChangelog().getChanges().get(0).getId());
        assertEquals(LAST_CHANGE_ID, issue.getChangelog().getChanges().get(99).getId());
    }

    @Test
    void addChangeLogShouldFetchRemainingWhenPartial() {
        IssuesService service = new IssuesService(context(DeploymentType.Server), ExportType.DETAILED);
        Issue issue = new Issue();
        issue.setKey(TEST_ISSUE_KEY);

        ChangeLog partialChangelog = new ChangeLog();
        partialChangelog.setMaxResults(PARTIAL_CHANGELOG_ENTRIES);
        partialChangelog.setTotal(TOTAL_CHANGELOG_ENTRIES);
        partialChangelog.setChanges(issueChanges(0, PARTIAL_CHANGELOG_ENTRIES));
        issue.setChangelog(partialChangelog);

        service.addChangeLog(issue);
        service.addChangeLog(issue);

        // Verify handler was called
        assertTrue(handlerCalled, "Handler should be called when changelog is partial");
        assertEquals(List.of(PARTIAL_CHANGELOG_ENTRIES), requestedStartIndexes);
        
        // Verify specific data was fetched
        assertEquals(TOTAL_CHANGELOG_ENTRIES, issue.getChangelog().getTotal());
        assertEquals(PARTIAL_CHANGELOG_ENTRIES, issue.getChangelog().getMaxResults());
        assertEquals(TOTAL_CHANGELOG_ENTRIES, issue.getChangelog().getChanges().size());
        assertEquals(FIRST_CHANGE_ID, issue.getChangelog().getChanges().get(0).getId());
        assertEquals("change-49", issue.getChangelog().getChanges().get(PARTIAL_CHANGELOG_ENTRIES - 1).getId());
        assertEquals("change-50", issue.getChangelog().getChanges().get(PARTIAL_CHANGELOG_ENTRIES).getId());
        assertEquals(LAST_CHANGE_ID, issue.getChangelog().getChanges().get(TOTAL_CHANGELOG_ENTRIES - 1).getId());
    }

    @Test
    void addChangeLogShouldNoOpWhenComplete() {
        // Prevent handler from being called
        allowHandlerCalls = false;
        handlerCalled = false;
        
        IssuesService service = new IssuesService(context(DeploymentType.Server), ExportType.DETAILED);
        Issue issue = new Issue();
        issue.setKey(TEST_ISSUE_KEY);

        ChangeLog completeChangelog = new ChangeLog();
        completeChangelog.setMaxResults(1);
        completeChangelog.setTotal(1);
        ArrayList<IssueChange> initialChanges = new ArrayList<>();
        IssueChange dummyChange = new IssueChange();
        dummyChange.setId("original-change");
        initialChanges.add(dummyChange);
        completeChangelog.setChanges(initialChanges);
        issue.setChangelog(completeChangelog);

        service.addChangeLog(issue);

        // Verify handler was NOT called
        assertFalse(handlerCalled, "Handler should not be called when changelog is complete");
        assertTrue(requestedStartIndexes.isEmpty());

        // Verify original data was preserved
        assertEquals(1, issue.getChangelog().getChanges().size());
        assertEquals("original-change", issue.getChangelog().getChanges().get(0).getId());
        assertEquals(1, issue.getChangelog().getMaxResults());
        assertEquals(1, issue.getChangelog().getTotal());
    }

    private ArrayList<IssueChange> issueChanges(int startIndex, int count) {
        ArrayList<IssueChange> changes = new ArrayList<>();
        for (int i = startIndex; i < startIndex + count; i++) {
            IssueChange change = new IssueChange();
            change.setId("change-" + i);
            changes.add(change);
        }
        return changes;
    }

    private JiraDeploymentContext context(DeploymentType type) {
        return new JiraDeploymentContext(jiraHome, NOOP, type);
    }
}
