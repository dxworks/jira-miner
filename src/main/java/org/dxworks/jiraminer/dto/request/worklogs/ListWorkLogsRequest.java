package org.dxworks.jiraminer.dto.request.worklogs;

import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListWorkLogsRequest {
    @Key
    private List<Long> ids;
}
