package org.dxworks.jiraminer.export;

import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssueFieldsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;

import java.util.List;

import static java.util.Collections.emptyList;

public class JiraRepoExporter {

    private String jiraHome;
    private String projectId;
    private AuthenticationProvider authenticator;
    private List<IssueField> customFields = emptyList();

    public JiraRepoExporter(String jiraHome, String projectId, AuthenticationProvider authenticator) {
        this.jiraHome = jiraHome;
        this.projectId = projectId;
        this.authenticator = authenticator;
    }

    public JiraRepoExporter(String jiraHome, String projectId, AuthenticationProvider authenticator, List<IssueField> customFields) {
        this.jiraHome = jiraHome;
        this.projectId = projectId;
        this.authenticator = authenticator;
        this.customFields = customFields;
    }

    public JiraProjectResult export() {
        IssuesService issuesService = new IssuesService(jiraHome, authenticator);
        IssueFieldsService issueFieldsService = new IssueFieldsService(jiraHome, authenticator);
        CommentsService commentsService = new CommentsService(jiraHome, authenticator);
        StatusesService statusesService = new StatusesService(jiraHome, authenticator);

        List<Issue> issues = issuesService.getAllIssuesForProjects(projectId);
        commentsService.addCommentsToIssues(issues);
        List<IssueField> issueFields = issueFieldsService.getFields();
        List<IssueStatus> allStatuses = statusesService.getAllStatuses();

        return new ResultExporter().getExportResult(issues, allStatuses, customFields);
    }
}
