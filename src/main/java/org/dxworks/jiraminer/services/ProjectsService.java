package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.projects.Project;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

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

        HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);
        return asList(httpResponse.parseAs(Project[].class));
    }

    @SneakyThrows
    public Project getProject(String projectKey) {

        String apiPath = getApiPath(ImmutableMap.of("projectKey", projectKey), "project", ":projectKey");

        HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);

        return httpResponse.parseAs(Project.class);
    }

}
