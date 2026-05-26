package org.dxworks.jiraminer.dto.request.issues;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class JiraCloudIssuesRequestBody {
    @Key
    private String jql;
    @Key
    private int maxResults;
    @Key
    private List<String> fields;
    @Key
    private String expand;
    @Key
    private String nextPageToken;
}
