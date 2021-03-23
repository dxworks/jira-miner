package org.dxworks.jiraminer.dto.response.issues.worklog;

import com.google.api.client.util.Key;
import lombok.Data;
import org.dxworks.jiraminer.pagination.Paginated;

import java.util.List;

@Data
public class WorkLogsResponse extends Paginated {

    @Key
    private List<WorkLog> worklogs;
}
