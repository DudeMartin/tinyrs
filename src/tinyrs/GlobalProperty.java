package tinyrs;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

public enum GlobalProperty {

    SCREENSHOT_FORMAT("png"),
    DEFAULT_WORLD(2),
    CONFIRM_CLOSE(false),
    ALWAYS_ON_TOP(false),
    REMEMBER_WINDOW_BOUNDS(false),
    LAST_WINDOW_X(0),
    LAST_WINDOW_Y(0),
    LAST_WINDOW_WIDTH(765),
    LAST_WINDOW_HEIGHT(503);

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
        final Class<?> thisType = defaultValue.getClass();
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

    public static Collection<String> readAll(final InputStream inputStream) {
        final Collection<String> unrecognizedProperties = new ArrayList<String>();
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
                try {
                    valueOf(line.substring(0, equalsIndex)).set(convertFromString(line.substring(equalsIndex + 1)));
                } catch (final IllegalArgumentException swallowed) {
                    unrecognizedProperties.add(line);
                }
            }
        } finally {
            scanner.close();
        }
        return unrecognizedProperties;
    }

    public static void writeAll(final OutputStream outputStream) {
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
