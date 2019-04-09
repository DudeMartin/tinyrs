package tinyrs.plugin;

import java.applet.Applet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

public abstract class Plugin {

    static final ThreadGroup pluginThreads = new ThreadGroup("Plugin Threads");
    private final AtomicBoolean pauseSignal = new AtomicBoolean();
    private final Object pauseLock = new Object();
    private JMenuItem menuItem;
    private boolean initialized;
    private boolean started;
    private boolean running;
    private volatile boolean shouldStop;
    private boolean paused;

    public final synchronized void initialize(final Applet applet) throws PluginException {
        if (isInitialized()) {
            throw new IllegalStateException("The plugin is already initialized.");
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    menuItem = createMenuItem();
                    if (menuItem == null) {
                        throw new NullPointerException("The plugin must provide a menu item.");
                    }
                }
            });
        } catch (final Exception e) {
            throw new PluginException("Failed to initialize the plugin menu item.", e);
        }
        initializePlugin(applet);
        initialized = true;
    }

    protected void initializePlugin(final Applet applet) throws PluginException {

    }

    public final synchronized void start() {
        if (!isInitialized()) {
            throw new IllegalStateException("The plugin must be initialized first.");
        } else if (isStarted()) {
            throw new IllegalStateException("The plugin has been started already.");
        }
        final Runnable executeTask = new Runnable() {

            @Override
            public void run() {
                running = true;
                try {
                    while (!shouldStop) {
                        if (pauseSignal.compareAndSet(true, false)) {
                            paused = true;
                            synchronized (pauseLock) {
                                try {
                                    pauseLock.wait();
                                } catch (final InterruptedException e) {
                                    e.printStackTrace();
                                } finally {
                                    paused = false;
                                }
                            }
                        }
                        final long sleepMillis = execute();
                        if (sleepMillis > 0) {
                            sleepUninterruptibly(sleepMillis);
                        }
                    }
                } finally {
                    running = false;
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            getMenuItem().setEnabled(false);
                        }
                    });
                }
            }
        };
        final Thread thread = new Thread(pluginThreads, executeTask, name());
        thread.setDaemon(true);
        thread.start();
        started = true;
    }

    public final void stop() {
        shouldStop = true;
    }

    public final void pause() {
        pauseSignal.compareAndSet(false, true);
    }

    public final void resume() {
        synchronized (pauseLock) {
            pauseLock.notify();
        }
    }

    public final JMenuItem getMenuItem() {
        return menuItem;
    }

    public final boolean isInitialized() {
        return initialized;
    }

    public final boolean isStarted() {
        return started;
    }

    public final boolean isRunning() {
        return running;
    }

    public final boolean isPaused() {
        return paused;
    }

    protected String name() {
        return getClass().getSimpleName();
    }

    protected abstract long execute();

    protected abstract JMenuItem createMenuItem();

    private static void sleepUninterruptibly(final long millis) {
        final long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException swallowed) {
            final long remainingMillis = millis - (System.currentTimeMillis() - startTime);
            if (remainingMillis > 0) {
                sleepUninterruptibly(remainingMillis);
            }
        }
    }
}
