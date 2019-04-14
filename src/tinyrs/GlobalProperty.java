package tinyrs;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.Set;

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

    private static final Set<GlobalProperty> PROPERTIES = Collections.unmodifiableSet(
            EnumSet.allOf(GlobalProperty.class));
    private final Object defaultValue;
    private volatile Object value;

    GlobalProperty(final Object defaultValue) {
        this.defaultValue = defaultValue;
        value = defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final Class<T> type) {
        assertCompatibleType(type);
        return (T) value;
    }

    public String get() {
        return get(Object.class).toString();
    }

    @SuppressWarnings("unchecked")
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
        if (!(type == Object.class
                || (thisType == Integer.class && type == int.class)
                || (thisType == Boolean.class && type == boolean.class)
                || type.isAssignableFrom(thisType))) {
            throw new IllegalArgumentException("The provided type is incompatible with this property's.");
        }
    }

    public static void readAll(final InputStream inputStream) {
        final Scanner scanner = new Scanner(inputStream);
        try {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine().trim();
                if (line.startsWith("#")) {
                    continue;
                }
                final int equalsIndex = line.indexOf('=');
                if (equalsIndex == -1) {
                    continue;
                }
                try {
                    getProperty(line.substring(0, equalsIndex)).set(convertFromString(line.substring(equalsIndex + 1)));
                } catch (final IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            scanner.close();
        }
    }

    public static void writeAll(final OutputStream outputStream) {
        final PrintWriter printWriter = new PrintWriter(outputStream);
        try {
            for (final GlobalProperty property : PROPERTIES) {
                printWriter.println(property.toString() + '=' + property.get());
            }
            printWriter.flush();
        } finally {
            printWriter.close();
        }
    }

    private static GlobalProperty getProperty(final String name) {
        for (final GlobalProperty property : PROPERTIES) {
            if (property.name().equalsIgnoreCase(name)) {
                return property;
            }
        }
        throw new IllegalArgumentException("There is no global property with the name \"" + name + "\".");
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
