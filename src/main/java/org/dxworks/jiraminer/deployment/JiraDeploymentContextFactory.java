package org.dxworks.jiraminer.deployment;

import java.util.Locale;

import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Optional;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Key;

public class JiraDeploymentContextFactory {

    public static JiraDeploymentContext detect(String jiraHome, HttpRequestInitializer requestInitializer) {
        ServerInfoService serverInfoService = new ServerInfoService(jiraHome, requestInitializer);
        String deploymentType = serverInfoService.getDeploymentType()
                .orElseThrow(() -> new IllegalStateException("Could not detect Jira deployment type from /serverInfo"));
        String normalizedDeploymentType = deploymentType.trim().toLowerCase(Locale.ROOT);
    
        if ("cloud".equals(normalizedDeploymentType)) {
            return new JiraDeploymentContext(jiraHome, requestInitializer, DeploymentType.Cloud);
        }
    
        if ("server".equals(normalizedDeploymentType)
                || "data center".equals(normalizedDeploymentType)
                || "datacenter".equals(normalizedDeploymentType)
                || "dc".equals(normalizedDeploymentType)) {
            return new JiraDeploymentContext(jiraHome, requestInitializer, DeploymentType.Server);
        }
    
        throw new IllegalStateException("Unsupported Jira deployment type: " + deploymentType);
    }

        static class ServerInfoService extends JiraApiService {
        private ServerInfoService(String jiraHome, HttpRequestInitializer requestInitializer) {
            super(jiraHome, requestInitializer);
        }

        Optional<String> getDeploymentType() {
            HttpResponse response = rlGet(new GenericUrl(getApiPath("serverInfo")));
            return parseIfOk(response, ServerInfoResponse.class)
                    .map(ServerInfoResponse::getDeploymentType);
        }
    }

    public static class ServerInfoResponse {
        @Key
        private String deploymentType;

        public String getDeploymentType() {
            return deploymentType;
        }

        public void setDeploymentType(String deploymentType) {
            this.deploymentType = deploymentType;
        }
    }
}
