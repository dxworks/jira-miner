package org.dxworks.jiraminer;

import lombok.SneakyThrows;
import org.dxworks.utils.java.rest.client.providers.BasicAuthenticationProvider;

import java.util.Properties;

public class TestUtils {

    private static Properties properties;

    public static String getProperty(String key) {
        if (properties == null)
            loadProperties();

        return properties.getProperty(key);
    }

    @SneakyThrows
    private static void loadProperties() {
        properties = new Properties();
        properties.load(TestUtils.class.getClassLoader().getResourceAsStream("fppt.properties"));
    }

    public static BasicAuthenticationProvider getGithubCredentials() {
        return new BasicAuthenticationProvider(getProperty("github.username"), getProperty("github.token"));
    }

    public static BasicAuthenticationProvider getJiraCredentials() {
        return new BasicAuthenticationProvider(getProperty("jira.username"), getProperty("jira.token"));
    }
}
