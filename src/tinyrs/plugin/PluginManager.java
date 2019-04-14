package tinyrs.plugin;

import java.applet.Applet;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

public final class PluginManager {

    private static final String JAR_URL_FORMAT = "jar:file:%s!/";
    private final Set<Plugin> plugins = Collections.newSetFromMap(new ConcurrentHashMap<Plugin, Boolean>());
    private JMenu pluginMenu;

    public PluginManager() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                pluginMenu = new JMenu("Plugins");
                pluginMenu.setVisible(false);
            }
        });
    }

    public void loadPlugin(final JarFile pluginArchive) throws IOException, PluginException {
        final Manifest pluginManifest = pluginArchive.getManifest();
        if (pluginManifest == null) {
            throw new PluginArchiveException("The plugin archive is missing the manifest file.");
        }
        final String pluginClassName = pluginManifest.getMainAttributes().getValue("Plugin-Class");
        if (pluginClassName == null) {
            throw new PluginArchiveException("The manifest file is missing the Plugin-Class attribute.");
        }
        final ClassLoader classLoader = URLClassLoader.newInstance(
                new URL[] { new URL(String.format(JAR_URL_FORMAT, pluginArchive.getName())) });
        final Plugin plugin;
        try {
            plugin = classLoader.loadClass(pluginClassName).asSubclass(Plugin.class).newInstance();
        } catch (final ClassNotFoundException e) {
            throw new PluginArchiveException("The plugin class could not be found.", e);
        } catch (final ClassCastException e) {
            throw new PluginArchiveException(
                    "The plugin class is not a subclass of " + Plugin.class.getName() + '.', e);
        } catch (final IllegalAccessException e) {
            throw new PluginArchiveException("The plugin class or its nullary constructor is not accessible.", e);
        } catch (final InstantiationException e) {
            throw new PluginArchiveException("The plugin class is abstract or is missing a nullary constructor.", e);
        } catch (final Exception e) {
            throw new PluginException("Failed to create an instance of the plugin.", e);
        }
        plugins.add(plugin);
    }

    public void startPlugins(final Applet gameApplet) {
        for (final Plugin plugin : plugins) {
            if (plugin.isStarted()) {
                continue;
            }
            new Thread(Plugin.pluginThreads, new Runnable() {

                @Override
                public void run() {
                    try {
                        if (!plugin.isInitialized()) {
                            plugin.initialize(gameApplet);
                        }
                        plugin.start();
                    } catch (final PluginException e) {
                        e.printStackTrace();
                        plugins.remove(plugin);
                        return;
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            pluginMenu.add(plugin.getMenuItem());
                            pluginMenu.validate();
                            pluginMenu.repaint();
                            pluginMenu.setVisible(true);
                        }
                    });
                }
            }, "Plugin Starter [" + plugin.name() + ']').start();
        }
    }

    public JMenu getPluginMenu() {
        return pluginMenu;
    }
}
