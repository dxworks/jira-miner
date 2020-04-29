package org.dxworks.jiraminer.configuration;

public class JiraMinerConfigValidation {

    public static void notNull(Object o, String message, Object... values) {
        if (o == null)
            throw new InvalidConfigurationException(String.format(message, values),
                    new NullPointerException());
    }
}
