package org.dxworks.jiraminer.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpRequestInitializer;
import com.sun.net.httpserver.HttpExchange;
import org.dxworks.jiraminer.ServerInfoTestBase;
import org.dxworks.jiraminer.configuration.ExportType;
import org.dxworks.jiraminer.deployment.DeploymentType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssuesServiceDeploymentRoutingTest extends ServerInfoTestBase {

    private static final HttpRequestInitializer NOOP = request -> {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int TOTAL_ISSUES = 150;
    private static final int PAGE_SIZE = 100;

    @Test
    void serverDeploymentShouldUseOffsetPagination() throws IOException {
        List<Integer> observedStartAts = new ArrayList<>();
        startServer(exchange -> {
            JsonNode requestBody = readRequestBody(exchange);
            int startAt = parseStartAt(requestBody);
            observedStartAts.add(startAt);
            sendJson(exchange, offsetResponse(startAt));
        });

        IssuesService service = new IssuesService(context(DeploymentType.Server), ExportType.BASIC);

        List<Issue> issues = service.getAllIssuesForProjects("TEST");

        assertEquals(TOTAL_ISSUES, issues.size());
        assertEquals("TEST-1", issues.get(0).getKey());
        assertEquals("TEST-100", issues.get(PAGE_SIZE - 1).getKey());
        assertEquals("TEST-101", issues.get(PAGE_SIZE).getKey());
        assertEquals("TEST-150", issues.get(TOTAL_ISSUES - 1).getKey());
        assertEquals(Arrays.asList(0, PAGE_SIZE), observedStartAts);
    }

    @Test
    void cloudDeploymentShouldUseTokenPagination() throws IOException {
        List<Integer> observedNextPageTokens = new ArrayList<>();
        List<String> observedPaths = new ArrayList<>();
        List<String> observedExpands = new ArrayList<>();
        List<String> observedFields = new ArrayList<>();
        List<Boolean> observedStartAtPresence = new ArrayList<>();
        startServer(exchange -> {
            JsonNode requestBody = readRequestBody(exchange);
            observedPaths.add(exchange.getRequestURI().getPath());
            int nextPageToken = parseNextPageToken(requestBody);
            observedNextPageTokens.add(nextPageToken);
            observedExpands.add(getText(requestBody, "expand"));
            observedFields.add(getFirstArrayItem(requestBody, "fields"));
            observedStartAtPresence.add(requestBody.has("startAt"));
            sendJson(exchange, tokenResponse(nextPageToken));
        });

        IssuesService service = new IssuesService(context(DeploymentType.Cloud), ExportType.DETAILED);

        List<Issue> issues = service.getAllIssuesForProjects("TEST");

        assertEquals(TOTAL_ISSUES, issues.size());
        assertEquals("TEST-1", issues.get(0).getKey());
        assertEquals("TEST-100", issues.get(PAGE_SIZE - 1).getKey());
        assertEquals("TEST-101", issues.get(PAGE_SIZE).getKey());
        assertEquals("TEST-150", issues.get(TOTAL_ISSUES - 1).getKey());
        assertEquals(Arrays.asList(0, PAGE_SIZE), observedNextPageTokens);
        assertEquals(Arrays.asList("/rest/api/3/search/jql", "/rest/api/3/search/jql"), observedPaths);
        assertEquals(Arrays.asList("changelog", "changelog"), observedExpands);
        assertEquals(Arrays.asList("*all", "*all"), observedFields);
        assertEquals(Arrays.asList(false, false), observedStartAtPresence);
    }

    // Server setup
    private void startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rest/api/2/search", handler);
        server.createContext("/rest/api/3/search/jql", handler);
        server.start();
        jiraHome = "http://localhost:" + server.getAddress().getPort();
    }

    private JiraDeploymentContext context(DeploymentType type) {
        return new JiraDeploymentContext(jiraHome, NOOP, type);
    }

    // Request parsing
    private JsonNode readRequestBody(HttpExchange exchange) throws IOException {
        return OBJECT_MAPPER.readTree(exchange.getRequestBody());
    }

    private int parseStartAt(JsonNode body) {
        return getInt(body, "startAt");
    }

    private int parseNextPageToken(JsonNode body) {
        String token = getText(body, "nextPageToken");
        return token.isEmpty() ? 0 : Integer.parseInt(token.substring(5));
    }

    private int getInt(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || node.isNull() ? 0 : node.asInt();
    }

    private String getText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private String getFirstArrayItem(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || !node.isArray() || node.isEmpty() ? "" : node.get(0).asText();
    }

    // Response building
    private String issuesJson(int start, int count) {
        return IntStream.range(start, start + count)
            .mapToObj(i -> "{\"key\":\"TEST-" + (i + 1) + "\"}")
            .collect(Collectors.joining(","));
    }

    private String offsetResponse(int startAt) {
        int count = Math.min(PAGE_SIZE, TOTAL_ISSUES - startAt);
        return "{\"startAt\":" + startAt + ",\"maxResults\":" + PAGE_SIZE + 
               ",\"total\":" + TOTAL_ISSUES + ",\"issues\":[" + issuesJson(startAt, count) + "]}";
    }

    private String tokenResponse(int startAt) {
        int count = Math.min(PAGE_SIZE, TOTAL_ISSUES - startAt);
        String issues = issuesJson(startAt, count);
        boolean hasMore = startAt + count < TOTAL_ISSUES;
        return hasMore 
            ? "{\"nextPageToken\":\"page_" + (startAt + PAGE_SIZE) + "\",\"issues\":[" + issues + "]}"
            : "{\"issues\":[" + issues + "]}";
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
