package tinyrs;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public enum GlobalProperty {

    SCREENSHOT_FORMAT("png"),
    DEFAULT_WORLD(2),
    CONFIRM_CLOSE(false),
    ALWAYS_ON_TOP(false);

    private static final Map<GlobalProperty, Object> globalProperties = new ConcurrentHashMap<GlobalProperty, Object>();
    static {
        for (final GlobalProperty property : values()) {
            globalProperties.put(property, property.defaultValue);
        }
    }

    private final Object defaultValue;
    private final Class<?> type;

    GlobalProperty(final Object defaultValue) {
        this.defaultValue = defaultValue;
        this.type = defaultValue.getClass();
    }

    GlobalProperty(final int defaultValue) {
        this.defaultValue = defaultValue;
        this.type = int.class;
    }

    GlobalProperty(final boolean defaultValue) {
        this.defaultValue = defaultValue;
        this.type = boolean.class;
    }

    public <T> T get(final Class<T> type) {
        assertCompatibleType(type);
        return (T) globalProperties.get(this);
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
        globalProperties.put(this, value);
    }

    public void setDefault() {
        globalProperties.put(this, defaultValue);
    }

    private void assertCompatibleType(final Class<?> type) {
        boolean compatible = type == Object.class || type.isAssignableFrom(this.type);
        if (this.type == int.class) {
            compatible |= Integer.class.isAssignableFrom(type);
        } else if (this.type == boolean.class) {
            compatible |= Boolean.class.isAssignableFrom(type);
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
                globalProperties.put(property, convertFromString(line.substring(equalsIndex + 1)));
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
                printWriter.println(property.toString() + '=' + globalProperties.get(property));
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
