package org.dxworks.jiraminer.pagination;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class Paginated {
    @Key
    private int startIndex;
    @Key
    private int maxResults;
    @Key
    private int total;
}
