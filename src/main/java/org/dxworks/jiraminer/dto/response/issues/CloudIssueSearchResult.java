package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Cloud-specific search result using token-based pagination.
 * Unlike Server's offset-based model, Cloud returns nextPageToken for pagination.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudIssueSearchResult {
    @Key
    private List<Issue> issues = Collections.emptyList();

    @Key
    private String nextPageToken;

    public boolean hasMore() {
        return nextPageToken != null && !nextPageToken.isEmpty();
    }
}
