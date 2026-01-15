package org.dxworks.jiraminer.main;

public class CustomFieldsExportConfig {

    public static final String EXPORT_CUSTOM_FIELDS = "exportCustomFields";

    public static boolean shouldExportCustomFields(String propertyValue) {
        if (propertyValue == null) {
            return true;
        }
        return Boolean.parseBoolean(propertyValue.trim());
    }
}
