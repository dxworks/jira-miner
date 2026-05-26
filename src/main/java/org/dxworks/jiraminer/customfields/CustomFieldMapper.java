package org.dxworks.jiraminer.customfields;

import com.google.api.client.util.Data;
import org.dxworks.jiraminer.dto.response.issues.Issue;
import org.dxworks.jiraminer.dto.response.issues.IssueField;
import org.dxworks.jiraminer.dto.response.issues.IssueFields;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CustomFieldMapper {

    private CustomFieldMapper() {
    }

    public static Map<String, Object> fromIssueFields(IssueFields fields, Map<String, String> fieldNamesById) {
        if (fields == null) {
            return null;
        }

        Map<String, Object> customFields = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldId = entry.getKey();
            if (fieldId == null || !fieldId.startsWith("customfield_")) {
                continue;
            }

            Object normalizedValue = normalizeValue(entry.getValue());
            if (normalizedValue == null) {
                continue;
            }

            String fieldName = fieldNamesById == null ? null : fieldNamesById.get(fieldId);
            customFields.put(toOutputKey(fieldId, fieldName), normalizedValue);
        }

        return customFields.isEmpty() ? null : customFields;
    }

    public static Map<String, Object> fromIssueAndDefinitions(Issue issue, List<IssueField> customFieldDefinitions) {
        Map<String, Object> customFields = new LinkedHashMap<>();
        if (issue == null || customFieldDefinitions == null) {
            return customFields;
        }

        for (IssueField field : customFieldDefinitions) {
            if (field == null) {
                continue;
            }

            String fieldId = field.getId();
            if (fieldId == null || !fieldId.startsWith("customfield_")) {
                continue;
            }

            Object normalizedValue = normalizeValue(issue.get(fieldId));
            if (normalizedValue == null) {
                continue;
            }

            customFields.put(toOutputKey(fieldId, field.getName()), normalizedValue);
        }

        return customFields;
    }

    private static String toOutputKey(String fieldId, String fieldName) {
        return fieldName == null || fieldName.isBlank() ? fieldId : fieldId + "|" + fieldName;
    }

    private static Object normalizeValue(Object value) {
        if (value == null || Data.isNull(value)) {
            return null;
        }

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Map) {
            Map<?, ?> mapValue = (Map<?, ?>) value;
            if (mapValue.containsKey("value")) {
                return normalizeValue(mapValue.get("value"));
            }
            if (mapValue.containsKey("name")) {
                return normalizeValue(mapValue.get("name"));
            }
            return mapValue;
        }

        if (value instanceof List) {
            List<?> listValue = (List<?>) value;
            if (listValue.isEmpty()) {
                return null;
            }

            List<Object> processedList = new ArrayList<>();
            for (Object item : listValue) {
                Object normalizedItem = normalizeListItem(item);
                if (normalizedItem != null) {
                    processedList.add(normalizedItem);
                }
            }

            return processedList.isEmpty() ? null : processedList;
        }

        String stringValue = value.toString();
        return stringValue.contains("@") ? null : stringValue;
    }

    private static Object normalizeListItem(Object item) {
        if (item == null || Data.isNull(item)) {
            return null;
        }

        if (item instanceof Map) {
            Map<?, ?> mapItem = (Map<?, ?>) item;
            if (mapItem.containsKey("value")) {
                return normalizeValue(mapItem.get("value"));
            }
            if (mapItem.containsKey("name")) {
                return normalizeValue(mapItem.get("name"));
            }
            return mapItem;
        }

        return item;
    }
}
