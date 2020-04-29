package org.dxworks.jiraminer.export;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.issues.CommentsService;
import org.dxworks.jiraminer.issues.IssueFieldsService;
import org.dxworks.jiraminer.issues.IssuesService;
import org.dxworks.utils.java.rest.client.providers.AuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class ResultExporterIT {

    private static final String JIRA_HOME = "https://inspectorgit.atlassian.net";
    private static final AuthenticationProvider authenticator = TestUtils.getJiraAuthenticator();

    private final IssuesService issuesService = new IssuesService(JIRA_HOME, authenticator);

    private final IssueFieldsService issueFieldsService = new IssueFieldsService(JIRA_HOME, authenticator);

    private final CommentsService commentsService = new CommentsService(JIRA_HOME, authenticator);

    @Test
    void exportIssues() {
        List<Issue> issues = issuesService.getAllIssuesForProjects("IG");
        issues.forEach(commentsService::addCommentsToIssue);
        List<IssueField> issueFields = issueFieldsService.getFields();

        List<IssueField> testCustomFields = issueFields.stream()
                .filter(issueField -> issueField.getId().equals("customfield_10026") || issueField.getId()
                        .equals("customfield_10018")).collect(Collectors.toList());

        new ResultExporter().export(issues, Paths.get("./tasks.json").toFile(), testCustomFields);
    }

}
