package tinyrs.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import tinyrs.GlobalProperty;
import tinyrs.utils.StreamUtility;

public final class BulkPluginLoader {

    private static final String JAR_URL_FORMAT = "jar:%s!/";

    private BulkPluginLoader() {
    }

    public static Map<String, Exception> loadPlugins(
            final PluginManager pluginManager,
            final String... pluginAddresses) {
        final Map<String, Exception> failureMap = new HashMap<String, Exception>();
        for (final String pluginAddress : pluginAddresses) {
            try {
                final URL pluginUrl;
                if (!pluginAddress.startsWith("jar:")) {
                    pluginUrl = new URL(String.format(JAR_URL_FORMAT,  pluginAddress));
                } else {
                    pluginUrl = new URL(pluginAddress);
                }
                final JarURLConnection pluginConnection = (JarURLConnection) pluginUrl.openConnection();
                pluginManager.loadPlugin(pluginConnection);
            } catch (final Exception e) {
                System.err.println("Failed to load the plugin at " + pluginAddress + ". Ignoring...");
                e.printStackTrace();
                failureMap.put(pluginAddress, e);
            }
        }
        return Collections.unmodifiableMap(failureMap);
    }

    public static Map<String, Exception> loadFromRepository(final PluginManager pluginManager) throws IOException {
        URL repositoryAddress;
        try {
            repositoryAddress = new URL(GlobalProperty.PLUGIN_REPOSITORY.get());
        } catch (final MalformedURLException e) {
            if (GlobalProperty.PLUGIN_REPOSITORY.getDefault().equals(GlobalProperty.PLUGIN_REPOSITORY.get())) {
                throw e;
            }
            System.err.println("Invalid plugin repository address. Re-setting to default and trying again...");
            e.printStackTrace();
            GlobalProperty.PLUGIN_REPOSITORY.setDefault();
            repositoryAddress = new URL(GlobalProperty.PLUGIN_REPOSITORY.get());
        }
        final InputStream repositoryStream = repositoryAddress.openStream();
        final byte[] repositorySourceBytes;
        try {
            repositorySourceBytes = StreamUtility.readBytes(repositoryStream);
        } finally {
            repositoryStream.close();
        }
        return loadPlugins(pluginManager, new String(repositorySourceBytes, Charset.forName("UTF-8")).split("\n"));
    }
}
