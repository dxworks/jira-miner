package org.dxworks.jiraminer.dto.response.worklogs;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;

import java.util.List;

@Data
public class UpdatedWorkLogsResponse extends GenericJson {
    @Key
    private Long since;
    @Key
    private Long until;

    @Key
    private List<WorklogValue> values;
}
