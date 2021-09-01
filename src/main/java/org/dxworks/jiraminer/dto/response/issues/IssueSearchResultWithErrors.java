package org.dxworks.jiraminer.dto.response.issues;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dxworks.utils.java.rest.client.response.HttpResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueSearchResultWithErrors extends IssueSearchResult {
    private HttpResponse httpResponse;
    private int startAt;
}
