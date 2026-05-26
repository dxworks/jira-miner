package org.dxworks.jiraminer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public abstract class ServerInfoTestBase {

    protected HttpServer server;
    protected String jiraHome;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    protected void startServerWithServerInfoHandler(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/rest/api/2/serverInfo", handler);
        server.start();
        jiraHome = "http://localhost:" + server.getAddress().getPort();
    }

    protected void respondWithoutDeploymentType(HttpExchange exchange) throws IOException {
        byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }

    protected void respondWithDeploymentType(HttpExchange exchange, String deploymentType) throws IOException {
        byte[] response = ("{\"deploymentType\":\"" + deploymentType + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
