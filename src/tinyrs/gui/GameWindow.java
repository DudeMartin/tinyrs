package tinyrs.gui;

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import tinyrs.Application;
import tinyrs.GlobalProperty;
import tinyrs.gui.menu.OpenStorageListener;
import tinyrs.gui.menu.SetDefaultWorldListener;
import tinyrs.gui.menu.TakeScreenshotListener;
import tinyrs.gui.utils.GamepackDownloadWorker;
import tinyrs.utils.AppletUtility;
import tinyrs.utils.StreamUtility;
import tinyrs.utils.VersionUtility;

public final class GameWindow extends JFrame {

    private static final Icon FOLDER_ICON = loadIcon("folder.png");
    private static final Icon CAMERA_ICON = loadIcon("camera.png");
    private static final Icon WORLD_ICON = loadIcon("world.png");
    private static final Icon CONFIRM_ICON = loadIcon("confirm.png");
    private static final ThreadGroup gameThreads = new ThreadGroup("Game Threads");
    private final CenteredTextPanel centerPanel = new CenteredTextPanel();
    private final AtomicBoolean started = new AtomicBoolean();

    public GameWindow() {
        final JMenuBar menuBar = new JMenuBar();
        final JMenu fileMenu = new JMenu("File");
        if (Application.isStorageDirectoryAvailable()) {
            if (Desktop.isDesktopSupported()) {
                final JMenuItem openDirectoryItem = new JMenuItem("Open storage directory", FOLDER_ICON);
                openDirectoryItem.addActionListener(new OpenStorageListener(openDirectoryItem, FOLDER_ICON));
                fileMenu.add(openDirectoryItem);
            }
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (final AWTException e) {
                e.printStackTrace();
                new PopupBuilder()
                        .withParent(this)
                        .withMessage("Could not initialize the facility for taking screenshots.")
                        .withTitle("Screenshot Error")
                        .withMessageType(JOptionPane.ERROR_MESSAGE)
                        .withIcon(CAMERA_ICON)
                        .showMessage();
            }
            if (robot != null) {
                final JMenuItem screenshotItem = new JMenuItem("Take screenshot", CAMERA_ICON);
                screenshotItem.addActionListener(new TakeScreenshotListener(centerPanel, CAMERA_ICON, robot));
                fileMenu.add(screenshotItem);
            }
            final JMenuItem defaultWorldItem = new JMenuItem("Set default world", WORLD_ICON);
            defaultWorldItem.addActionListener(new SetDefaultWorldListener(defaultWorldItem, WORLD_ICON));
            fileMenu.add(defaultWorldItem);
        }
        final JCheckBoxMenuItem confirmCloseItem = new JCheckBoxMenuItem(
                "Confirm on close",
                CONFIRM_ICON,
                GlobalProperty.CONFIRM_CLOSE.get(boolean.class));
        confirmCloseItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                GlobalProperty.CONFIRM_CLOSE.set(confirmCloseItem.isSelected());
            }
        });
        fileMenu.add(confirmCloseItem);
        final JMenuItem defaultSizeItem = new JMenuItem("Set default size", loadIcon("resize.png"));
        defaultSizeItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                centerPanel.setPreferredSize(new Dimension(765, 503));
                pack();
            }
        });
        fileMenu.add(defaultSizeItem);
        final JCheckBoxMenuItem alwaysOnTopItem = new JCheckBoxMenuItem(
                "Always on top",
                loadIcon("top.png"),
                GlobalProperty.ALWAYS_ON_TOP.get(boolean.class));
        alwaysOnTopItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                GlobalProperty.ALWAYS_ON_TOP.set(alwaysOnTopItem.isSelected());
                setAlwaysOnTop(alwaysOnTopItem.isSelected());
            }
        });
        fileMenu.add(alwaysOnTopItem);
        final boolean rememberBounds = GlobalProperty.REMEMBER_WINDOW_BOUNDS.get(boolean.class);
        final JCheckBoxMenuItem rememberBoundsItem = new JCheckBoxMenuItem(
                "Remember window bounds",
                loadIcon("bounds.png"),
                rememberBounds);
        rememberBoundsItem.setToolTipText("Remember the window position and size after closing it.");
        rememberBoundsItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(final ItemEvent e) {
                GlobalProperty.REMEMBER_WINDOW_BOUNDS.set(rememberBoundsItem.isSelected());
            }
        });
        fileMenu.add(rememberBoundsItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        if (rememberBounds) {
            setLocation(GlobalProperty.LAST_WINDOW_X.get(int.class), GlobalProperty.LAST_WINDOW_Y.get(int.class));
            centerPanel.setPreferredSize(new Dimension(
                    GlobalProperty.LAST_WINDOW_WIDTH.get(int.class),
                    GlobalProperty.LAST_WINDOW_HEIGHT.get(int.class)));
        } else {
            centerPanel.setPreferredSize(new Dimension(765, 503));
        }
        centerPanel.showText("Loading...");
        centerPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(final ComponentEvent e) {
                GlobalProperty.LAST_WINDOW_WIDTH.set(centerPanel.getWidth());
                GlobalProperty.LAST_WINDOW_HEIGHT.set(centerPanel.getHeight());
            }
        });
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(final ComponentEvent e) {
                final Point screenPosition = getLocationOnScreen();
                GlobalProperty.LAST_WINDOW_X.set(screenPosition.x);
                GlobalProperty.LAST_WINDOW_Y.set(screenPosition.y);
            }
        });
        add(centerPanel);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(final WindowEvent e) {
                if (started.compareAndSet(false, true)) {
                    loadGame();
                }
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                if (GlobalProperty.CONFIRM_CLOSE.get(boolean.class)) {
                    final int closeOption = new PopupBuilder()
                            .withParent(GameWindow.this)
                            .withMessage("Are you sure you want to close?")
                            .withTitle("Confirm Close")
                            .withMessageType(JOptionPane.QUESTION_MESSAGE)
                            .withIcon(CONFIRM_ICON)
                            .showYesNoInput();
                    if (closeOption != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                System.exit(0);
            }
        });
    }

    private void showErrorText(final String message) {
        centerPanel.removeAll();
        centerPanel.validate();
        centerPanel.showText(message);
    }

    private void loadGame() {
        if (Application.isStorageDirectoryAvailable()) {
            final File gamepackFile = new File(Application.storageDirectory(), "gamepack.jar");
            if (gamepackFile.exists()) {
                new SwingWorker<Boolean, Void>() {

                    @Override
                    protected Boolean doInBackground() throws Exception {
                        return VersionUtility.isLatestRevision(VersionUtility.getRevision(new JarFile(gamepackFile)));
                    }

                    @Override
                    protected void done() {
                        boolean latestRevision;
                        try {
                            latestRevision = get();
                        } catch (final Exception e) {
                            e.printStackTrace();
                            latestRevision = false;
                        }
                        if (latestRevision) {
                            try {
                                startGame(gamepackFile.toURI().toURL());
                            } catch (final MalformedURLException impossible) {
                                throw new Error(impossible);
                            }
                        } else {
                            downloadThenStartGame(gamepackFile);
                        }
                    }
                }.execute();
            } else {
                downloadThenStartGame(gamepackFile);
            }
        } else {
            try {
                startGame(
                        new URL("http", AppletUtility.getHostForWorld(GlobalProperty.DEFAULT_WORLD.get(int.class)), "/gamepack.jar"));
            } catch (final MalformedURLException impossible) {
                throw new Error(impossible);
            }
        }
    }

    private void downloadThenStartGame(final File gamepackFile) {
        final JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        centerPanel.add(progressBar);
        centerPanel.validate();
        centerPanel.showTextAbove("Downloading...", progressBar, 15);
        new GamepackDownloadWorker(gamepackFile, progressBar) {

            @Override
            protected void done() {
                try {
                    get();
                } catch (final Exception e) {
                    e.printStackTrace();
                    showErrorText("Could not download the game client.");
                    return;
                }
                try {
                    startGame(gamepackFile.toURI().toURL());
                } catch (final MalformedURLException impossible) {
                    throw new Error(impossible);
                }
            }
        }.execute();
    }

    private void startGame(final URL gamepackAddress) {
        new SwingWorker<Applet, Void>() {

            @Override
            protected Applet doInBackground() throws Exception {
                final ClassLoader classLoader = new URLClassLoader(new URL[] { gamepackAddress });
                final Applet gameApplet = (Applet) classLoader.loadClass("client").newInstance();
                final URL codeBase = new URL("http", AppletUtility.getHostForWorld(GlobalProperty.DEFAULT_WORLD.get(int.class)), "/");
                final String pageSource = new String(StreamUtility.readBytes(codeBase.openStream()), "ISO-8859-1");
                gameApplet.setStub(AppletUtility.createActiveStub(codeBase, AppletUtility.parseParameters(pageSource)));
                return gameApplet;
            }

            @Override
            protected void done() {
                final Applet gameApplet;
                try {
                    gameApplet = get();
                } catch (final Exception e) {
                    e.printStackTrace();
                    showErrorText("Could not start the game client.");
                    return;
                }
                centerPanel.removeAll();
                centerPanel.setLayout(new BorderLayout());
                centerPanel.add(gameApplet);
                centerPanel.validate();
                pack();
                new Thread(gameThreads, new Runnable() {

                    @Override
                    public void run() {
                        gameApplet.init();
                        gameApplet.start();
                    }
                }, "Game Starter").start();
            }
        }.execute();
    }

    private static ImageIcon loadIcon(final String name) {
        return new ImageIcon(GameWindow.class.getResource("/resources/" + name));
    }
}
