package tinyrs.plugin;

public class PluginException extends Exception {

    PluginException(final String message) {
        super(message);
    }

    PluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
