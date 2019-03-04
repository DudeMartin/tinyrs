package tinyrs.plugin;

import java.applet.Applet;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public final class PluginManager {

    private static final String JAR_URL_FORMAT = "jar:file:%s!/";
    private final JMenu pluginMenu = new JMenu("Plugins");
    private final Map<Plugin, JMenuItem> pluginMap = new HashMap<Plugin, JMenuItem>();

    public PluginManager() {
        pluginMenu.setVisible(false);
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
        final ClassLoader classLoader = new URLClassLoader(
                new URL[] { new URL(String.format(JAR_URL_FORMAT, pluginArchive.getName())) });
        final Class<?> mainPluginClass;
        try {
            mainPluginClass = classLoader.loadClass(pluginClassName);
        } catch (final ClassNotFoundException e) {
            throw new PluginArchiveException("The plugin class specified in the manifest file could not be found.", e);
        }
        if (!Plugin.class.isAssignableFrom(mainPluginClass)) {
            throw new PluginArchiveException(
                    "The plugin class specified in the manifest file is not a subclass of " + Plugin.class.getName() + '.');
        }
        final Class<? extends Plugin> pluginClass = mainPluginClass.asSubclass(Plugin.class);
        final Constructor<? extends Plugin> nullaryConstructor;
        try {
            nullaryConstructor = pluginClass.getDeclaredConstructor();
        } catch (final NoSuchMethodException e) {
            throw new PluginArchiveException(
                    "The plugin class specified in the manifest file is missing a nullary constructor.", e);
        }
        final Plugin plugin;
        try {
            plugin = nullaryConstructor.newInstance();
        } catch (final Exception e) {
            throw new PluginException("Failed to initialize the plugin.", e);
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    final JMenuItem pluginMenuItem = plugin.createMenuItem();
                    if (pluginMenuItem == null) {
                        throw new NullPointerException();
                    }
                    pluginMap.put(plugin, pluginMenuItem);
                    addPlugin(plugin);
                }
            });
        } catch (final Exception e) {
            throw new PluginException("Failed to create and add the plugin menu item.", e);
        }
    }

    public void startPlugins(final Applet gameApplet) {
        for (final Plugin plugin : pluginMap.keySet()) {
            try {
                plugin.start(gameApplet);
            } catch (final Exception e) {
                e.printStackTrace();
                continue;
            }
            addPlugin(plugin);
        }
    }

    public void stopPlugins() {
        for (final Plugin plugin : pluginMap.keySet()) {
            try {
                plugin.stop();
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                removePlugin(plugin);
            }
        }
    }

    public JMenu getPluginMenu() {
        return pluginMenu;
    }

    private void addPlugin(final Plugin plugin) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                final JMenuItem pluginMenuItem = pluginMap.get(plugin);
                pluginMenu.add(pluginMenuItem);
                if (pluginMenu.getMenuComponentCount() > 0) {
                    pluginMenu.setVisible(true);
                }
                pluginMenu.validate();
                pluginMenu.repaint();
                pluginMap.remove(plugin);
            }
        });
    }

    private void removePlugin(final Plugin plugin) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                final JMenuItem pluginMenuItem = pluginMap.get(plugin);
                pluginMenu.remove(pluginMenuItem);
                if (pluginMenu.getMenuComponentCount() == 0) {
                    pluginMenu.setVisible(false);
                }
                pluginMenu.validate();
                pluginMenu.repaint();
                pluginMap.remove(plugin);
            }
        });
    }
}
