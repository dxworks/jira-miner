package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;
import org.dxworks.jiraminer.pagination.Paginated;

import java.util.List;

@Data
public class ChangeLog extends Paginated {
    @Key("histories")
    private List<IssueChange> changes;
}
