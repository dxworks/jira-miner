package org.dxworks.jiraminer.projects;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.projects.Project;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Properties;

import static java.util.Arrays.asList;

public class ProjectsService extends JiraApiService {

    public ProjectsService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public ProjectsService(String jiraHome) {
        super(jiraHome);
    }

    @SneakyThrows
    public List<Project> getAllProjects() {

        String apiPath = getApiPath("project");

        HttpResponse httpResponse = httpClient.get(new GenericUrl(apiPath));
        return asList(httpResponse.parseAs(Project[].class));
    }

    @SneakyThrows
    public Project getProject(String projectKey) {

        String apiPath = getApiPath(ImmutableMap.of("projectKey", projectKey), "project", ":projectKey");

        HttpResponse httpResponse = httpClient.get(new GenericUrl(apiPath));

        return httpResponse.parseAs(Project.class);
    }

}
