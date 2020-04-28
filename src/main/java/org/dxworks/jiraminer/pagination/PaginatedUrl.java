package org.dxworks.jiraminer.pagination;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.util.Key;
import lombok.Data;

@Data
public class PaginatedUrl extends GenericUrl {
    @Key
    private int startIndex;
    @Key
    private int maxResults;

    public PaginatedUrl(String encodedUrl, int startIndex, int maxResults) {
        super(encodedUrl);
        this.startIndex = startIndex;
        this.maxResults = maxResults;
    }
}
