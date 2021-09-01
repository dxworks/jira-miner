package org.dxworks.jiraminer.dto.request.issues;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@ToString
public class JiraIssuesRequestBody {
    @Key
    private String jql;
    @Key
    private int startAt;
    @Key
    private int maxResults;
    @Key
    private List<String> expand;
}
