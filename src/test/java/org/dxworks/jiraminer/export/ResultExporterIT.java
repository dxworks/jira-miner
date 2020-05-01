package org.dxworks.jiraminer.export;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.jiraminer.services.CommentsService;
import org.dxworks.jiraminer.services.IssueFieldsService;
import org.dxworks.jiraminer.services.IssuesService;
import org.dxworks.jiraminer.services.StatusesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class ResultExporterIT {

    private static final String jiraHome = TestUtils.getJiraHome();
    private static final AuthenticationProvider authenticator = TestUtils.getJiraAuthenticator();

    private final IssuesService issuesService = new IssuesService(jiraHome, authenticator);
    private final IssueFieldsService issueFieldsService = new IssueFieldsService(jiraHome, authenticator);
    private final CommentsService commentsService = new CommentsService(jiraHome, authenticator);
    private final StatusesService statusesService = new StatusesService(jiraHome, authenticator);

    @Test
    void exportIssues() {
        List<Issue> issues = issuesService.getAllIssuesForProjects("IG");
        issues.forEach(commentsService::addCommentsToIssue);
        List<IssueField> issueFields = issueFieldsService.getFields();
        List<IssueStatus> allStatuses = statusesService.getAllStatuses();

        List<IssueField> testCustomFields = issueFields.stream()
                .filter(issueField -> issueField.getId().equals("customfield_10026") || issueField.getId()
                        .equals("customfield_10018")).collect(Collectors.toList());

        new ResultExporter().export(issues, allStatuses, Paths.get("./tasks.json").toFile(), testCustomFields);
    }

}
