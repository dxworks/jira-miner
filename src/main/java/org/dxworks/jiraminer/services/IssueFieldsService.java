package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class IssueFieldsService extends JiraApiService {

    public IssueFieldsService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public IssueFieldsService(String jiraHome) {
        super(jiraHome);
    }

    @SneakyThrows
    public List<IssueField> getFields() {
        String apiPath = getApiPath("field");

        HttpResponse httpResponse = getHttpClient().get(new GenericUrl(apiPath), null);

        return parseListIfOk(httpResponse, IssueField[].class);
    }
}
