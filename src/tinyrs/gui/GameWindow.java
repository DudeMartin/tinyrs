package tinyrs.gui;

import java.applet.Applet;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import tinyrs.Application;
import tinyrs.GlobalProperty;
import tinyrs.gui.menu.OpenStorageListener;
import tinyrs.gui.menu.SetDefaultWorldListener;
import tinyrs.gui.menu.TakeScreenshotListener;
import tinyrs.utils.AppletUtility;
import tinyrs.utils.StreamUtility;
import tinyrs.utils.VersionUtility;

public class GameWindow extends JFrame {

    private static final Icon FOLDER_ICON = loadIcon("folder.png");
    private static final Icon CAMERA_ICON = loadIcon("camera.png");
    private static final Icon WORLD_ICON = loadIcon("world.png");
    private final JPanel centerPanel = new CenterPanel();
    private boolean started;

    public GameWindow() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        if (Application.storageDirectory() != null) {
            if (Desktop.isDesktopSupported()) {
                JMenuItem openDirectoryItem = new JMenuItem("Open storage directory", FOLDER_ICON);
                openDirectoryItem.addActionListener(new OpenStorageListener(openDirectoryItem, FOLDER_ICON));
                fileMenu.add(openDirectoryItem);
            }
            Robot robot = null;
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
                new PopupBuilder()
                        .withParent(GameWindow.this)
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
        final JCheckBoxMenuItem confirmCloseItem = new JCheckBoxMenuItem("Confirm on close",
                loadIcon("confirm.png"),
                GlobalProperty.CONFIRM_CLOSE.get(boolean.class));
        confirmCloseItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                GlobalProperty.CONFIRM_CLOSE.set(confirmCloseItem.isSelected());
            }
        });
        fileMenu.add(confirmCloseItem);
        JMenuItem defaultSizeItem = new JMenuItem("Set default size", loadIcon("resize.png"));
        defaultSizeItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                pack();
            }
        });
        fileMenu.add(defaultSizeItem);
        final JCheckBoxMenuItem alwaysOnTopItem = new JCheckBoxMenuItem("Always on top",
                loadIcon("top.png"),
                GlobalProperty.ALWAYS_ON_TOP.get(boolean.class));
        alwaysOnTopItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                GlobalProperty.ALWAYS_ON_TOP.set(alwaysOnTopItem.isSelected());
                setAlwaysOnTop(alwaysOnTopItem.isSelected());
            }
        });
        fileMenu.add(alwaysOnTopItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        Dimension defaultSize = new Dimension(700, 500);
        setSize(defaultSize);
        setPreferredSize(defaultSize);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        centerPanel.setBackground(Color.BLACK);
        add(centerPanel);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowActivated(WindowEvent windowEvent) {
                if (!started) {
                    loadGame();
                }
            }

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                boolean confirmClose = GlobalProperty.CONFIRM_CLOSE.get(boolean.class);
                if (confirmClose) {
                    int closeOption = new PopupBuilder()
                            .withParent(GameWindow.this)
                            .withMessage("Are you sure you want to close?")
                            .withTitle("Confirm Close")
                            .withMessageType(JOptionPane.QUESTION_MESSAGE)
                            .showYesNoInput();
                    if (closeOption != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                System.exit(0);
            }
        });
    }

    private void loadGame() {
        started = true;
        final File storageDirectory = Application.storageDirectory();
        if (storageDirectory == null) {
            JLabel loadingLabel = new JLabel("Loading...");
            loadingLabel.setForeground(Color.WHITE);
            centerPanel.add(loadingLabel);
            centerPanel.validate();
            startGameClient(defaultGamepackAddress());
        } else {
            File gamepackFile = new File(storageDirectory, "gamepack.jar");
            if (gamepackFile.exists()) {
                boolean latestRevision;
                try {
                    latestRevision = VersionUtility.isLatestRevision(VersionUtility.getRevision(new JarFile(gamepackFile)));
                } catch (IOException e) {
                    e.printStackTrace();
                    latestRevision = false;
                }
                if (latestRevision) {
                    try {
                        startGameClient(gamepackFile.toURI().toURL());
                    } catch (MalformedURLException impossible) {
                        throw new Error(impossible);
                    }
                    return;
                }
            }
            JProgressBar progressBar = new JProgressBar();
            progressBar.setStringPainted(true);
            centerPanel.add(progressBar);
            centerPanel.validate();
            centerPanel.repaint();
            new GamepackDownloadWorker(gamepackFile, progressBar).execute();
        }
    }

    private void startGameClient(final URL gamepackAddress) {
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
                    new PopupBuilder()
                            .withParent(GameWindow.this)
                            .withMessage("Could not start the game client.")
                            .withTitle("Game Error")
                            .withMessageType(JOptionPane.ERROR_MESSAGE)
                            .showMessage();
                    return;
                }
                centerPanel.removeAll();
                centerPanel.setLayout(new BorderLayout());
                centerPanel.setPreferredSize(new Dimension(765, 503));
                centerPanel.add(gameApplet);
                centerPanel.validate();
                gameApplet.init();
                gameApplet.start();
                setPreferredSize(null);
                pack();
            }
        }.execute();
    }

    private static ImageIcon loadIcon(String name) {
        return new ImageIcon(GameWindow.class.getResource("/resources/" + name));
    }

    private static URL defaultGamepackAddress() {
        try {
            return new URL("http", AppletUtility.getHostForWorld(GlobalProperty.DEFAULT_WORLD.get(int.class)), "/gamepack.jar");
        } catch (MalformedURLException impossible) {
            throw new Error(impossible);
        }
    }

    private class CenterPanel extends JPanel {

        private CenterPanel() {
            super(new GridBagLayout());
            setBackground(Color.BLACK);
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getComponentCount() > 0) {
                Component component = getComponent(0);
                if (component instanceof JProgressBar) {
                    FontMetrics metrics = g.getFontMetrics();
                    g.setColor(Color.WHITE);
                    g.drawString("Downloading game...",
                            component.getX() + component.getWidth() / 2 - metrics.stringWidth("Downloading game...") / 2,
                            component.getY() - 15 + metrics.getHeight() / 2);
                }
            }
        }
    }

    private class GamepackDownloadWorker extends SwingWorker<Void, Integer> {

        private final File destinationFile;
        private final JProgressBar progressBar;

        private GamepackDownloadWorker(final File destinationFile, final JProgressBar progressBar) {
            this.destinationFile = destinationFile;
            this.progressBar = progressBar;
        }

        @Override
        protected Void doInBackground() throws Exception {
            final URLConnection gamepackConnection = defaultGamepackAddress().openConnection();
            final int gamepackSize = gamepackConnection.getContentLength();
            publish(gamepackSize == -1 ? Integer.MIN_VALUE : 0);
            final InputStream gamepackStream = gamepackConnection.getInputStream();
            final byte[] gamepackBytes;
            if (gamepackSize == -1) {
                gamepackBytes = StreamUtility.readBytes(gamepackStream);
            } else {
                final AtomicInteger totalBytesRead = new AtomicInteger();
                gamepackBytes = StreamUtility.readBytes(gamepackStream, new StreamUtility.ProgressListener() {

                    @Override
                    public void onBytesRead(final int amount) {
                        publish(100 * totalBytesRead.addAndGet(amount) / gamepackSize);
                    }
                });
            }
            final OutputStream fileStream = new FileOutputStream(destinationFile);
            try {
                fileStream.write(gamepackBytes);
            } finally {
                fileStream.close();
            }
            return null;
        }

        @Override
        protected void process(final List<Integer> chunks) {
            for (final Integer percentage : chunks) {
                if (percentage == Integer.MIN_VALUE) {
                    progressBar.setIndeterminate(true);
                    return;
                }
                progressBar.setValue(percentage);
            }
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (final Exception e) {
                e.printStackTrace();
                new PopupBuilder()
                        .withParent(GameWindow.this)
                        .withMessage("Could not download the game client.")
                        .withTitle("Download Error")
                        .withMessageType(JOptionPane.ERROR_MESSAGE)
                        .showMessage();
                return;
            }
            try {
                startGameClient(destinationFile.toURI().toURL());
            } catch (MalformedURLException impossible) {
                throw new Error(impossible);
            }
        }
    }
}