package tinyrs.plugin;

public class PluginArchiveException extends PluginException {

    PluginArchiveException(final String message) {
        super(message);
    }

    PluginArchiveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
