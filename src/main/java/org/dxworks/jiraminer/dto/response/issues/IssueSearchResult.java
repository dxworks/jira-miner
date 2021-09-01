package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dxworks.jiraminer.pagination.Paginated;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueSearchResult extends Paginated {
    @Key
    private List<Issue> issues = Collections.emptyList();
}
