package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.deployment.JiraDeploymentContext;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

public class IssueFieldsService extends JiraApiService {

    public IssueFieldsService(JiraDeploymentContext context) {
        super(context.getJiraHome(), context.getApiVersion(), context.getRequestInitializer());
    }

    @SneakyThrows
    public List<IssueField> getFields() {
        String apiPath = getApiPath("field");

        HttpResponse httpResponse = rlGet(new GenericUrl(apiPath));

        return parseListIfOk(httpResponse, IssueField[].class);
    }
}
