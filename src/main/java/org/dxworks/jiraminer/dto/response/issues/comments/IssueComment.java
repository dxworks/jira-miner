package org.dxworks.jiraminer.dto.response.issues.comments;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dxworks.jiraminer.dto.response.users.User;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class IssueComment extends GenericJson {
    @Key
    private String id;
    @Key
    private Object body;
    @Key
    private String created;
    @Key
    private String updated;
    @Key
    private User author;
    @Key
    private User updateAuthor;

    public String getBody() {
        return extractText(body);
    }

    private String extractText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String bodyText) {
            return bodyText;
        }
        if (value instanceof Map<?, ?> mapValue) {
            return extractTextFromMap(mapValue);
        }
        if (value instanceof List<?> listValue) {
            return extractTextFromList(listValue);
        }
        return String.valueOf(value);
    }

    private String extractTextFromMap(Map<?, ?> mapValue) {
        if (mapValue.containsKey("text")) {
            Object textValue = mapValue.get("text");
            return textValue == null ? null : String.valueOf(textValue);
        }
        if (!mapValue.containsKey("content")) {
            return null;
        }
        return extractText(mapValue.get("content"));
    }

    private String extractTextFromList(List<?> listValue) {
        StringBuilder textBuilder = new StringBuilder();
        for (Object item : listValue) {
            String extracted = extractText(item);
            if (extracted == null || extracted.isEmpty()) {
                continue;
            }
            textBuilder.append(extracted);
        }
        return textBuilder.isEmpty() ? null : textBuilder.toString();
    }

    public String getUserId() {
        return getAuthor() != null ? getAuthor().getSelf() : null;
    }
}
