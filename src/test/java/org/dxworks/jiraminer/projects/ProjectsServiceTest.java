package org.dxworks.jiraminer.projects;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.projects.Project;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectsServiceTest {

    private static final String JIRA_HOME = "https://loose.atlassian.net";

    private ProjectsService projectsService;

    @Test
    void getAllProjects() {
        projectsService = new ProjectsService(JIRA_HOME, TestUtils.getJiraCredentials());

        List<Project> projects = projectsService.getAllProjects();

        assertTrue(projects.size() > 0);
    }
}
