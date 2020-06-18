package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import lombok.SneakyThrows;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.List;

import static java.util.Arrays.asList;

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

        HttpResponse httpResponse = httpClient.get(new GenericUrl(apiPath));

        return asList(httpResponse.parseAs(IssueField[].class));
    }
}
