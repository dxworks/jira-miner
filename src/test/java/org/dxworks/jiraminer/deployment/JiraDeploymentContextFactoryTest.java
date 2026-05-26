package org.dxworks.jiraminer.deployment;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.jiraminer.ServerInfoTestBase;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JiraDeploymentContextFactoryTest extends ServerInfoTestBase {

    private static final HttpRequestInitializer NOOP_INITIALIZER = request -> {};

    @Test
    void detectShouldFailWhenServerInfoDoesNotContainAUsableDeploymentType() throws IOException {
        startServerWithServerInfoHandler(this::respondWithoutDeploymentType);

        assertThrows(IllegalStateException.class,
                () -> JiraDeploymentContextFactory.detect(jiraHome, NOOP_INITIALIZER));
    }

    @Test
    void detectShouldMapDataCenterDeploymentTypeToServer() throws IOException {
        startServerWithServerInfoHandler(exchange -> respondWithDeploymentType(exchange, "Data Center"));

        JiraDeploymentContext context = JiraDeploymentContextFactory.detect(jiraHome, NOOP_INITIALIZER);

        assertEquals(DeploymentType.Server, context.getDeploymentType());
    }
}
