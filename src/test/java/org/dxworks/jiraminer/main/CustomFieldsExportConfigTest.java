package org.dxworks.jiraminer.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomFieldsExportConfigTest {

    @Test
    void shouldExportCustomFieldsByDefaultWhenPropertyIsMissing() {
        assertTrue(CustomFieldsExportConfig.shouldExportCustomFields(null));
    }

    @Test
    void shouldNotExportCustomFieldsWhenPropertyIsFalse() {
        assertFalse(CustomFieldsExportConfig.shouldExportCustomFields("false"));
    }
}
