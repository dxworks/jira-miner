package org.dxworks.jiraminer.export;

import com.google.api.client.http.HttpRequestInitializer;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.issues.IssueCommentsService;
import org.dxworks.jiraminer.issues.IssueFieldsService;
import org.dxworks.jiraminer.issues.IssuesService;
import org.dxworks.utils.java.rest.client.providers.CookieAuthenticationProvider;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class ResultExporterIT {

    private static final String JIRA_HOME = "https://inspectorgit.atlassian.net";
    private final HttpRequestInitializer jiraCredentials = new CookieAuthenticationProvider("ajs_group_id=null; ajs_anonymous_id=%22fe3ccbb8-27df-4b4f-99ea-c09e19fb1620%22; atlassian.xsrf.token=58bec222-f700-4503-826f-fae3f6ab98d9_f110e79a3aeb60ff1bef55646bd510c65e0dbf67_lin; cloud.session.token=eyJraWQiOiJzZXNzaW9uLXNlcnZpY2VcL3Nlc3Npb24tc2VydmljZSIsImFsZyI6IlJTMjU2In0.eyJhc3NvY2lhdGlvbnMiOltdLCJzdWIiOiI1NTcwNTg6N2VjNTg2YTQtOTdkNC00NjBjLTliYmItNTc1OTlmMDdmYjAzIiwiZW1haWxEb21haW4iOiJnbWFpbC5jb20iLCJpbXBlcnNvbmF0aW9uIjpbXSwiY3JlYXRlZCI6MTU4NzQyMDgyOCwicmVmcmVzaFRpbWVvdXQiOjE1ODc5MjUzMzQsInZlcmlmaWVkIjp0cnVlLCJpc3MiOiJzZXNzaW9uLXNlcnZpY2UiLCJzZXNzaW9uSWQiOiJlOTBmZDZmNi0yODJlLTQ1YWItYTBiZC1mOWM0MTRjMTNhNWIiLCJhdWQiOiJhdGxhc3NpYW4iLCJuYmYiOjE1ODc5MjQ3MzQsImV4cCI6MTU5MDUxNjczNCwiaWF0IjoxNTg3OTI0NzM0LCJlbWFpbCI6Im1hcmlvLnJpdmlzQGdtYWlsLmNvbSIsImp0aSI6ImU5MGZkNmY2LTI4MmUtNDVhYi1hMGJkLWY5YzQxNGMxM2E1YiJ9.ItKK40hbEFTYpoMElt92NurwXVrIdeuqgeUWhrEvybrWwea83Gpdm4-IwCnE_66YfZtJWBoY1oS3sVxhQ7HO07Dz0JSOMXHDx5JVR0HCUMvD-SJjT6v1q8HcGUIlerCAUGyho3xy8RaUGXY2lp0BND3C5mxwmKpkYk-dP1NIuY4yKe4euijETYU2aAHYmYECUaarVcrLZodE3q-Sd7pZjk9PkXGeMD-sO_PvGeWfAYjM7_XRrnQdR7vewHrHvJ_r1_Dc8MW_4XqgxMDAU0gankZzoxEya9IFSwCFq1b39lqDTUE4UyZQrVQX3psBA3PPcMCR_TU3-yFwxKiBA5Y3yg");

    private final IssuesService issuesService = new IssuesService(JIRA_HOME, jiraCredentials);

    private final IssueFieldsService issueFieldsService = new IssueFieldsService(JIRA_HOME, jiraCredentials);

    private final IssueCommentsService issueCommentsService = new IssueCommentsService(JIRA_HOME, jiraCredentials);

    @Test
    void exportIssues() {
        List<Issue> issues = issuesService.getAllIssuesForProjects("IG");
        issues.forEach(issueCommentsService::addCommentsToIssue);
        List<IssueField> issueFields = issueFieldsService.getFields();

        List<IssueField> testCustomFields = issueFields.stream()
                .filter(issueField -> issueField.getId().equals("customfield_10026") || issueField.getId()
                        .equals("customfield_10018")).collect(Collectors.toList());

        new ResultExporter().export(issues, Paths.get("./tasks.json").toFile(), testCustomFields);
    }

}
