package tinyrs.plugin;

import java.applet.Applet;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

public final class PluginManager {

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

    public void loadPlugin(final JarURLConnection pluginConnection) throws IOException, PluginException {
        final Attributes pluginAttributes = pluginConnection.getMainAttributes();
        if (pluginAttributes == null) {
            throw new PluginArchiveException("The plugin archive is missing the manifest file or main attributes.");
        }
        final String pluginClassName = pluginAttributes.getValue("Plugin-Class");
        if (pluginClassName == null) {
            throw new PluginArchiveException("The manifest file is missing the Plugin-Class attribute.");
        }
        final ClassLoader classLoader = URLClassLoader.newInstance(new URL[] { pluginConnection.getJarFileURL() });
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
            if (!plugin.isStarted()) {
                new PluginStarterThread(plugin, gameApplet).start();
            }
        }
    }

    public JMenu getPluginMenu() {
        return pluginMenu;
    }

    private final class PluginStarterThread extends Thread {

        private final Plugin plugin;
        private final Applet gameApplet;

        private PluginStarterThread(final Plugin plugin, final Applet gameApplet) {
            super(Plugin.pluginThreads, "Plugin Starter [" + plugin.name() + ']');
            this.plugin = plugin;
            this.gameApplet = gameApplet;
            setDaemon(true);
        }

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
    }
}
