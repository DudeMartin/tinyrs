package tinyrs;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public enum GlobalProperty {

    SCREENSHOT_FORMAT("png"),
    DEFAULT_WORLD(2),
    CONFIRM_CLOSE(false),
    ALWAYS_ON_TOP(false);

    private final Object defaultValue;
    private volatile Object value;

    GlobalProperty(final Object defaultValue) {
        this.defaultValue = defaultValue;
        value = defaultValue;
    }

    public <T> T get(final Class<T> type) {
        assertCompatibleType(type);
        return (T) value;
    }

    public String get() {
        return get(Object.class).toString();
    }

    public <T> T getDefault(final Class<T> type) {
        assertCompatibleType(type);
        return (T) defaultValue;
    }

    public String getDefault() {
        return getDefault(Object.class).toString();
    }

    public void set(final Object value) {
        assertCompatibleType(value.getClass());
        this.value = value;
    }

    public void setDefault() {
        value = defaultValue;
    }

    private void assertCompatibleType(final Class<?> type) {
        final Class<?> thisType = value.getClass();
        boolean compatible = type == Object.class || type.isAssignableFrom(thisType);
        if (thisType == Integer.class) {
            compatible |= type == int.class;
        } else if (thisType == Boolean.class) {
            compatible |= type == boolean.class;
        }
        if (!compatible) {
            throw new IllegalArgumentException("The provided type is incompatible with this property's.");
        }
    }

    public static Set<String> read(final InputStream inputStream) {
        final Set<String> unrecognizedProperties = new HashSet<String>();
        final Scanner scanner = new Scanner(inputStream);
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.startsWith("#")) {
                    continue;
                }
                final int equalsIndex = line.indexOf('=');
                if (equalsIndex == -1) {
                    continue;
                }
                final String propertyName = line.substring(0, equalsIndex);
                final GlobalProperty property;
                try {
                    property = valueOf(propertyName);
                } catch (final IllegalArgumentException swallowed) {
                    unrecognizedProperties.add(propertyName);
                    continue;
                }
                property.set(convertFromString(line.substring(equalsIndex + 1)));
            }
        } finally {
            scanner.close();
        }
        return unrecognizedProperties;
    }

    public static void write(final OutputStream outputStream) {
        final PrintWriter printWriter = new PrintWriter(outputStream);
        try {
            for (final GlobalProperty property : values()) {
                printWriter.println(property.toString() + '=' + property.get());
            }
            printWriter.println();
        } finally {
            printWriter.close();
        }
    }

    private static Object convertFromString(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException expected) {
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return value;
    }
}
