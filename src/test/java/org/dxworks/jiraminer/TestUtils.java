package org.dxworks.jiraminer;

import org.dxworks.jiraminer.configuration.JiraMinerConfiguration;
import org.dxworks.jiraminer.configuration.JiraMinerConfigurer;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.deployment.JiraDeploymentContextFactory;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;

public class TestUtils {

    public static String getProperty(String key) {
        return JiraMinerConfiguration.getInstance().getProperty(key);
    }

    public static AuthenticationProvider getJiraAuthenticator() {
        return JiraMinerConfigurer.getAuthenticator(JiraMinerConfiguration.getInstance());
    }

    public static String getJiraHome() {
        return JiraMinerConfiguration.getInstance().getJiraHome();
    }

    public static JiraDeploymentContext getDeploymentContext() {
        String jiraHome = getJiraHome();
        return JiraDeploymentContextFactory.detect(jiraHome, getJiraAuthenticator());
    }

    public static JiraDeploymentContext getDeploymentContext(String jiraHome) {
        return JiraDeploymentContextFactory.detect(jiraHome, getJiraAuthenticator());
    }
}
