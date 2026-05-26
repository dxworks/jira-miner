package org.dxworks.jiraminer.export;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.configuration.ExportType;
import org.dxworks.jiraminer.deployment.JiraDeploymentContextFactory;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssueFieldsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class DetailedResultMapperIT {

    private static final String jiraHome = TestUtils.getJiraHome();
    private static final AuthenticationProvider authenticator = TestUtils.getJiraAuthenticator();

    @Test
    void exportIssues() {
        JiraDeploymentContext deploymentContext = JiraDeploymentContextFactory.detect(jiraHome, authenticator);
        
        IssuesService issuesService = new IssuesService(deploymentContext, ExportType.DETAILED);
        IssueFieldsService issueFieldsService = new IssueFieldsService(deploymentContext);
        CommentsService commentsService = new CommentsService(deploymentContext);
        StatusesService statusesService = new StatusesService(deploymentContext);

        List<Issue> issues = issuesService.getAllIssuesForProjects("IG");
        commentsService.addCommentsToIssues(issues);
        List<IssueField> issueFields = issueFieldsService.getFields();
        List<IssueStatus> allStatuses = statusesService.getAllStatuses();

        List<IssueField> testCustomFields = issueFields.stream()
                .filter(issueField -> issueField.getId().equals("customfield_10026") || issueField.getId()
                        .equals("customfield_10018")).collect(Collectors.toList());

        DetailedResultMapper mapper = new DetailedResultMapper(deploymentContext);
        JiraProjectResult result = mapper.getExportResult(issues, allStatuses, testCustomFields);
        new JsonFileWriter().write(Paths.get("./tasks.json").toFile(), result);
    }

}
