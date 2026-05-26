package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.comments.IssueStatus;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

@Slf4j
public class StatusesService extends JiraApiService {
    public StatusesService(JiraDeploymentContext context) {
        super(context.getJiraHome(), context.getApiVersion(), context.getRequestInitializer());
    }

    @SneakyThrows
    public List<IssueStatus> getAllStatuses() {
        log.info("Getting statuses.");
        HttpResponse response = rlGet(new GenericUrl(getApiPath("status")));
        return parseListIfOk(response, IssueStatus[].class);
    }
}
