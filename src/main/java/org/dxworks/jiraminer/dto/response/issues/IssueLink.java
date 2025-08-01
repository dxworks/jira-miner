package org.dxworks.jiraminer.dto.response.issues;

import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class IssueLink {
    @Key
    private String id;
    
    @Key
    private LinkType type;
    
    @Key
    private LinkedIssue inwardIssue;
    
    @Key
    private LinkedIssue outwardIssue;
    
    @Data
    public static class LinkType {
        @Key
        private String id;
        
        @Key
        private String name;
        
        @Key
        private String inward;
        
        @Key
        private String outward;
    }
    
    @Data
    public static class LinkedIssue {
        @Key
        private String id;
        
        @Key
        private String key;
        
        @Key
        private String self;
    }
}
