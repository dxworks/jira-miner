package org.dxworks.jiraminer.deployment;

import com.google.api.client.http.HttpRequestInitializer;
import java.util.Objects;

public class JiraDeploymentContext {
    private final String jiraHome;
    private final HttpRequestInitializer requestInitializer;
    private final DeploymentType deploymentType;

    public JiraDeploymentContext(String jiraHome, HttpRequestInitializer requestInitializer, DeploymentType deploymentType) {
        this.jiraHome = Objects.requireNonNull(jiraHome);
        this.requestInitializer = Objects.requireNonNull(requestInitializer);
        this.deploymentType = Objects.requireNonNull(deploymentType);
    }

    public String getJiraHome() {
        return jiraHome;
    }

    public HttpRequestInitializer getRequestInitializer() {
        return requestInitializer;
    }

    public DeploymentType getDeploymentType() {
        return deploymentType;
    }

    public String getApiVersion() {
        return deploymentType == DeploymentType.Cloud ? "3" : "2";
    }
}
