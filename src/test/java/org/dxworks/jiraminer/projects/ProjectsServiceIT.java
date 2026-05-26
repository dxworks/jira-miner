package org.dxworks.jiraminer.projects;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.projects.Project;
import org.dxworks.jiraminer.services.ProjectsService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectsServiceIT {

    private static final String JIRA_HOME = "https://loose.atlassian.net";

    private ProjectsService projectsService;

    @Test
    void getAllProjects() {
        JiraDeploymentContext deploymentContext = TestUtils.getDeploymentContext(JIRA_HOME);
        projectsService = new ProjectsService(deploymentContext);

        List<Project> projects = projectsService.getAllProjects();

        assertTrue(projects.size() > 0);
    }
}
