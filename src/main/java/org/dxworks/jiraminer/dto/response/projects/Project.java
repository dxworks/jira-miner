package org.dxworks.jiraminer.dto.response.projects;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class Project {

    @Key
    private String key;
    @Key
    private String name;
}
