package tinyrs.plugin;

import java.applet.Applet;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public final class PluginManager {

    private static final String JAR_URL_FORMAT = "jar:file:%s!/";
    private final Map<Plugin, JMenuItem> pluginMap = new ConcurrentHashMap<Plugin, JMenuItem>();
    private final JMenu pluginMenu = new JMenu("Plugins");

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
        final Class<?> specifiedPluginClass;
        try {
            specifiedPluginClass = classLoader.loadClass(pluginClassName);
        } catch (final ClassNotFoundException e) {
            throw new PluginArchiveException("The plugin class specified in the manifest file could not be found.", e);
        }
        if (!Plugin.class.isAssignableFrom(specifiedPluginClass)) {
            throw new PluginArchiveException(
                    "The plugin class specified in the manifest file is not a subclass of " + Plugin.class.getName() + '.');
        }
        final Class<? extends Plugin> pluginClass = specifiedPluginClass.asSubclass(Plugin.class);
        final Constructor<? extends Plugin> nullaryPluginConstructor;
        try {
            nullaryPluginConstructor = pluginClass.getDeclaredConstructor();
        } catch (final NoSuchMethodException e) {
            throw new PluginArchiveException(
                    "The plugin class specified in the manifest file is missing a nullary constructor.", e);
        }
        final Plugin plugin;
        try {
            plugin = nullaryPluginConstructor.newInstance();
        } catch (final Exception e) {
            throw new PluginException("Failed to create an instance of the plugin.", e);
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
            addPluginToMenu(plugin);
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

    private void addPluginToMenu(final Plugin plugin) {
        executeOnEventDispatchThread(new Runnable() {

            @Override
            public void run() {
                final JMenuItem pluginMenuItem = pluginMap.get(plugin);
                pluginMenu.add(pluginMenuItem);
                if (pluginMenu.getMenuComponentCount() > 0) {
                    pluginMenu.setVisible(true);
                }
                pluginMenu.validate();
                pluginMenu.repaint();
            }
        });
    }

    private void removePlugin(final Plugin plugin) {
        executeOnEventDispatchThread(new Runnable() {

            @Override
            public void run() {
                final JMenuItem pluginMenuItem = pluginMap.remove(plugin);
                pluginMenu.remove(pluginMenuItem);
                if (pluginMenu.getMenuComponentCount() == 0) {
                    pluginMenu.setVisible(false);
                }
                pluginMenu.validate();
                pluginMenu.repaint();
            }
        });
    }

    private static void executeOnEventDispatchThread(final Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
