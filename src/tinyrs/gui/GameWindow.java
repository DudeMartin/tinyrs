package tinyrs.gui;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;
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
import tinyrs.utils.AppletUtility;
import tinyrs.utils.StreamUtility;
import tinyrs.utils.VersionUtility;

public class GameWindow extends JFrame {

    private final JPanel centerPanel = new CenterPanel();
    private boolean started;

    public GameWindow() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        final File storageDirectory = Application.storageDirectory;
        if (storageDirectory != null) {
            if (Desktop.isDesktopSupported()) {
                JMenuItem openDirectoryItem = new JMenuItem("Open storage directory", loadIcon("folder.png"));
                openDirectoryItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            Desktop.getDesktop().open(storageDirectory);
                        } catch (Exception e) {
                            e.printStackTrace();
                            new PopupBuilder()
                                    .withParent(GameWindow.this)
                                    .withMessage("Could not open the storage directory.")
                                    .withTitle("Directory Error")
                                    .withMessageType(JOptionPane.WARNING_MESSAGE)
                                    .showMessage();
                        }
                    }
                });
                fileMenu.add(openDirectoryItem);
            }
            final JMenuItem screenshotItem = new JMenuItem("Take screenshot", loadIcon("camera.png"));
            screenshotItem.addActionListener(new ActionListener() {

                private final DateFormat dateFormat = new SimpleDateFormat("dd.MM.YYYY.HHmm.ss");
                private final File screenshotDirectory = new File(storageDirectory, "Screenshots");
                private Robot robot;

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (robot == null) {
                        try {
                            robot = new Robot();
                        } catch (AWTException e) {
                            screenshotItem.setEnabled(false);
                            screenshotItem.removeActionListener(this);
                            e.printStackTrace();
                            new PopupBuilder()
                                    .withParent(GameWindow.this)
                                    .withMessage("Could not initialize the facility for taking screenshots.")
                                    .withTitle("Screenshot Error")
                                    .withMessageType(JOptionPane.ERROR_MESSAGE)
                                    .showMessage();
                            return;
                        }
                    }
                    if (screenshotDirectory.exists() || (screenshotDirectory.mkdirs() && screenshotDirectory.canRead())) {
                        final Rectangle visibleArea = centerPanel.getGraphicsConfiguration().getBounds().intersection(new Rectangle(
                                        centerPanel.getLocationOnScreen(),
                                        new Dimension(centerPanel.getWidth(), centerPanel.getHeight())));
                        new SwingWorker<Void, Void>() {

                            @Override
                            protected Void doInBackground() throws Exception {
                                Thread.sleep(250);
                                String screenshotFormat = GlobalProperty.SCREENSHOT_FORMAT.get();
                                if (!ImageIO.write(robot.createScreenCapture(visibleArea),
                                        screenshotFormat,
                                        new File(screenshotDirectory, "Screenshot-" + dateFormat.format(new Date()) + '.' + screenshotFormat))) {
                                    GlobalProperty.SCREENSHOT_FORMAT.setDefault();
                                }
                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    new PopupBuilder()
                                            .withParent(GameWindow.this)
                                            .withMessage("Could not take a screenshot.")
                                            .withTitle("Screenshot Error")
                                            .withMessageType(JOptionPane.WARNING_MESSAGE)
                                            .showMessage();
                                }
                            }
                        }.execute();
                    }
                }
            });
            fileMenu.add(screenshotItem);
            JMenuItem defaultWorldItem = new JMenuItem("Set default world", loadIcon("world.png"));
            defaultWorldItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String input = new PopupBuilder()
                            .withParent(GameWindow.this)
                            .withMessage("Please enter a world number.")
                            .withTitle("Enter World")
                            .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .showTextInput();
                    if (input != null && !input.isEmpty()) {
                        int world;
                        try {
                            world = Integer.parseInt(input);
                        } catch (NumberFormatException expected) {
                            new PopupBuilder()
                                    .withParent(GameWindow.this)
                                    .withMessage("Please enter a positive integer.")
                                    .withTitle("Input Error")
                                    .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                                    .showMessage();
                            return;
                        }
                        if (AppletUtility.isValidWorld(world)) {
                            GlobalProperty.DEFAULT_WORLD.set(input);
                        } else {
                            new PopupBuilder()
                                    .withParent(GameWindow.this)
                                    .withMessage("This world is unreachable or does not exist.")
                                    .withTitle("World Error")
                                    .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                                    .showMessage();
                        }
                    }
                }
            });
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
                    loadGame(storageDirectory);
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

    private void loadGame(File storageDirectory) {
        started = true;
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

    private void startGameClient(URL gamepackAddress) {
        final ClassLoader classLoader = new URLClassLoader(new URL[] { gamepackAddress });
        new SwingWorker<Applet, Void>() {

            @Override
            protected Applet doInBackground() throws Exception {
                Applet gameApplet = (Applet) classLoader.loadClass("client").newInstance();
                AppletStub gameAppletStub = new AppletStub() {

                    private final URL pageAddress =
                            new URL("http", AppletUtility.getHostForWorld(GlobalProperty.DEFAULT_WORLD.get(int.class)), "/");
                    private final Map<String, String> parameters =
                            AppletUtility.parseParameters(new String(StreamUtility.readBytes(pageAddress.openStream())));

                    @Override
                    public boolean isActive() {
                        return true;
                    }

                    @Override
                    public URL getDocumentBase() {
                        return pageAddress;
                    }

                    @Override
                    public URL getCodeBase() {
                        return pageAddress;
                    }

                    @Override
                    public String getParameter(String name) {
                        return parameters.get(name);
                    }

                    @Override
                    public AppletContext getAppletContext() {
                        return null;
                    }

                    @Override
                    public void appletResize(int width, int height) {

                    }
                };
                gameApplet.setStub(gameAppletStub);
                return gameApplet;
            }

            @Override
            protected void done() {
                Applet gameApplet;
                try {
                    gameApplet = get();
                } catch (Exception e) {
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
        try {
            return new ImageIcon(GameWindow.class.getResource("/resources/" + name));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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