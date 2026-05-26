package org.dxworks.jiraminer.export;

import org.dxworks.utils.java.rest.client.utils.JsonMapper;

import java.io.File;
import java.nio.charset.Charset;

public class JsonFileWriter {

    private final JsonMapper jsonMapper = new JsonMapper();

    public void write(File outputFile, Object value) {
        try {
            jsonMapper.writeJSONtoFile(outputFile, value, Charset.defaultCharset());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write JSON results to file " + outputFile, e);
        }
    }
}
