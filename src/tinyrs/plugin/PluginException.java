package tinyrs.plugin;

public class PluginException extends Exception {

    public PluginException(final String message) {
        super(message);
    }

    public PluginException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
