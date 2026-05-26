package org.dxworks.jiraminer.main;

import org.dxworks.jiraminer.ServerInfoTestBase;
import org.dxworks.jiraminer.configuration.JiraMinerConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MainDeploymentDetectionTest extends ServerInfoTestBase {

    @Test
    void mainShouldFailFastWhenDeploymentDetectionFails() throws IOException {
        // Given a Jira server that returns no usable deployment type
        startServerWithServerInfoHandler(this::respondWithoutDeploymentType);

        // And configuration pointing to that server
        JiraMinerConfiguration config = JiraMinerConfiguration.getInstance();
        config.setJiraHome(jiraHome);
        config.setProjects(Collections.singletonList("TEST"));
        config.setProjectId("TEST");
        config.setUseCache(false);

        // When/Then main should fail fast with IllegalStateException
        assertThrows(IllegalStateException.class, () -> Main.main(new String[]{}));
    }
}
