package org.dxworks.jiraminer;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JiraApiServiceTest {

    private static final String JIRA_HOME = "https://loose.atlassian.net";
    JiraApiService jiraApiService;

    @Test
    void getApiPathWithDefaultVersion() {
        jiraApiService = new JiraApiService(JIRA_HOME);
        Map<String, String> map = ImmutableMap.of("projectKey", "SM");

        assertEquals("https://loose.atlassian.net/rest/api/2/project/SM/properties", jiraApiService.getApiPath(map, "project", ":projectKey", "properties"));
    }

    @Test
    void getApiPathWithCustomVersion() {
        jiraApiService = new JiraApiService(JIRA_HOME, "3");
        Map<String, String> map = ImmutableMap.of("projectKey", "SM");

        assertEquals("https://loose.atlassian.net/rest/api/3/project/SM/properties", jiraApiService.getApiPath(map, "project", ":projectKey", "properties"));
    }
}
