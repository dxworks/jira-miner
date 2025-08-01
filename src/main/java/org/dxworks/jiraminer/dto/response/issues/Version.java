package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class Version {
    @Key
    private String id;
    
    @Key
    private String name;
    
    @Key
    private String description;
    
    @Key
    private Boolean archived;
    
    @Key
    private Boolean released;
    
    @Key
    private String releaseDate;
}
