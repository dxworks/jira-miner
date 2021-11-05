package org.dxworks.jiraminer.services;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Key;
import lombok.Data;
import org.dxworks.jiraminer.JiraApiService;
import org.dxworks.jiraminer.dto.request.worklogs.ListWorkLogsRequest;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.dxworks.jiraminer.dto.response.worklogs.UpdatedWorkLogsResponse;
import org.dxworks.jiraminer.dto.response.worklogs.WorklogValue;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WorklogService extends JiraApiService {
    public WorklogService(String jiraHome) {
        super(jiraHome);
    }

    public WorklogService(String jiraHome, String apiVersion) {
        super(jiraHome, apiVersion);
    }

    public WorklogService(String jiraHome, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, httpRequestInitializer);
    }

    public WorklogService(String jiraHome, String apiVersion, HttpRequestInitializer httpRequestInitializer) {
        super(jiraHome, apiVersion, httpRequestInitializer);
    }

    public List<Long> getWorkLogIdsModifiedSince(Long since) {
        String apiPath = getApiPath("worklog", "updated");
        HttpResponse httpResponse = getHttpClient().get(new UpdatedWorkLogsUrl(apiPath, since), null);

        return parseIfOk(httpResponse, UpdatedWorkLogsResponse.class)
            .map(UpdatedWorkLogsResponse::getValues)
            .orElseGet(Collections::emptyList)
            .stream()
            .map(WorklogValue::getWorklogId)
            .collect(Collectors.toList());

    }

    public List<WorkLog> listWorkLogsForIds(List<Long> ids) {
        String apiPath = getApiPath("worklog", "list");
        HttpResponse httpResponse = getHttpClient().post(new GenericUrl(apiPath), new ListWorkLogsRequest(ids), null);

        return parseListIfOk(httpResponse, WorkLog[].class);
    }

}

@Data
class UpdatedWorkLogsUrl extends GenericUrl {
    @Key
    private Long since;

    public UpdatedWorkLogsUrl(String encodedUrl, Long since) {
        super(encodedUrl);
        this.since = since;
    }
}
