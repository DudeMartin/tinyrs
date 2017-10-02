package me.mtus.tinyrs;

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
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

class GameWindow extends JFrame {

    private final JPanel centerPanel = new JPanel(new GridBagLayout());
    private boolean started;

    GameWindow() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        final String storageDirectory = Application.properties.getProperty("storageDirectory");
        if (storageDirectory != null) {
            if (Desktop.isDesktopSupported()) {
                JMenuItem openDirectoryItem = new JMenuItem("Open storage directory", loadIcon("folder.png"));
                openDirectoryItem.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        try {
                            Desktop.getDesktop().open(new File(storageDirectory));
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(GameWindow.this,
                                    "Could not open the storage directory.",
                                    "Directory Error",
                                    JOptionPane.WARNING_MESSAGE);
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
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(GameWindow.this,
                                    "Could not initialize the facility for taking screenshots.",
                                    "Screenshot Error",
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    if (screenshotDirectory.exists() || (screenshotDirectory.mkdirs() && screenshotDirectory.canRead())) {
                        final Point startPoint = centerPanel.getLocationOnScreen();
                        final Rectangle canvasBounds = centerPanel.getBounds();
                        new SwingWorker<Void, Void>() {

                            @Override
                            protected Void doInBackground() throws Exception {
                                Thread.sleep(250);
                                BufferedImage screenshot = robot.createScreenCapture(new Rectangle(
                                        startPoint.x,
                                        startPoint.y,
                                        canvasBounds.width,
                                        canvasBounds.height));
                                String screenshotFormat = Application.properties.getProperty("screenshotFormat");
                                if (!ImageIO.write(screenshot,
                                        screenshotFormat,
                                        new File(screenshotDirectory, "Screenshot-" + dateFormat.format(new Date()) + '.' + screenshotFormat))) {
                                    Application.properties.setProperty("screenshotFormat", "png");
                                }
                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    JOptionPane.showMessageDialog(GameWindow.this,
                                            "Could not take a screenshot.",
                                            "Screenshot Error",
                                            JOptionPane.WARNING_MESSAGE);
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
                    String input = JOptionPane.showInputDialog(GameWindow.this,
                            "Please enter a world number.",
                            "Enter World",
                            JOptionPane.INFORMATION_MESSAGE);
                    if (input != null && !input.isEmpty()) {
                        int world;
                        try {
                            world = Integer.parseInt(input);
                        } catch (NumberFormatException expected) {
                            JOptionPane.showMessageDialog(GameWindow.this,
                                    "Please enter an integer.",
                                    "Input Error",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        try {
                            InetAddress.getByName("oldschool" + world + ".runescape.com");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(GameWindow.this,
                                    "This world is unreachable or does not exist.",
                                    "World Error",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        Application.properties.setProperty("defaultWorld", input);
                    }
                }
            });
            fileMenu.add(defaultWorldItem);
        }
        final JCheckBoxMenuItem confirmCloseItem = new JCheckBoxMenuItem("Confirm on close",
                loadIcon("confirm.png"),
                Boolean.valueOf(Application.properties.getProperty("confirmClose")));
        confirmCloseItem.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                Application.properties.setProperty("confirmClose", Boolean.toString(confirmCloseItem.isSelected()));
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
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        setSize(700, 500);
        setPreferredSize(new Dimension(700, 500));
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
                if (Boolean.valueOf(Application.properties.getProperty("confirmClose", "false")) == Boolean.TRUE &&
                        JOptionPane.showOptionDialog(GameWindow.this,
                                "Are you sure you want to close?",
                                "Confirm Close",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, null, null, null) != JOptionPane.YES_OPTION) {
                    return;
                }
                Application.saveProperties();
                System.exit(0);
            }
        });
    }

    private void loadGame(String storageDirectory) {
        started = true;
        if (storageDirectory == null) {
            JLabel loadingLabel = new JLabel("Loading...");
            loadingLabel.setForeground(Color.WHITE);
            centerPanel.add(loadingLabel);
            centerPanel.validate();
            startGameClient(defaultGamepackAddress());
        } else {
            File gamepackFile = new File(storageDirectory, "gamepack.jar");
            if (gamepackFile.exists() && GameHelpers.isLatestRevision(gamepackFile)) {
                try {
                    startGameClient(gamepackFile.toURI().toURL());
                } catch (MalformedURLException impossible) {
                    throw new Error(impossible);
                }
            } else {
                JProgressBar progressBar = new JProgressBar();
                progressBar.setStringPainted(true);
                centerPanel.add(progressBar);
                centerPanel.validate();
                new GamepackDownloadWorker(gamepackFile, progressBar).execute();
            }
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
                            new URL("http://oldschool" + Application.properties.getProperty("defaultWorld") + ".runescape.com");
                    private final Map<String, String> parameters =
                            GameHelpers.parseParameters(new String(GameHelpers.readStream(pageAddress.openStream())));

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
                    JOptionPane.showMessageDialog(GameWindow.this,
                            "Could not start the game client.",
                            "Game Error",
                            JOptionPane.ERROR_MESSAGE);
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
            return new URL("http://oldschool" + Application.properties.getProperty("defaultWorld") + ".runescape.com/gamepack.jar");
        } catch (MalformedURLException impossible) {
            throw new Error(impossible);
        }
    }

    private class GamepackDownloadWorker extends SwingWorker<Void, Integer> {

        private final File gamepackFile;
        private final JProgressBar progressBar;

        private GamepackDownloadWorker(File gamepackFile, JProgressBar progressBar) {
            this.gamepackFile = gamepackFile;
            this.progressBar = progressBar;
        }

        @Override
        protected Void doInBackground() throws Exception {
            URLConnection gamepackConnection = defaultGamepackAddress().openConnection();
            int fileSize = gamepackConnection.getContentLength();
            InputStream inputStream = gamepackConnection.getInputStream();
            byte[] gamepackBytes;
            try {
                if (fileSize == -1) {
                    publish(-1);
                    gamepackBytes = GameHelpers.readStream(inputStream);
                } else {
                    gamepackBytes = new byte[fileSize];
                    for (int index = 0, bytesRead;
                         index < fileSize && (bytesRead = inputStream.read(gamepackBytes, index, fileSize - index)) >= 0;
                         index += bytesRead, publish(index * 100 / fileSize));
                }
            } finally {
                inputStream.close();
            }
            OutputStream fileStream = new FileOutputStream(gamepackFile);
            try {
                fileStream.write(gamepackBytes);
            } finally {
                fileStream.close();
            }
            return null;
        }

        @Override
        protected void process(List<Integer> list) {
            for (Integer percentage : list) {
                if (percentage == -1) {
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
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(GameWindow.this,
                        "Could not download the game client.",
                        "Download Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                startGameClient(gamepackFile.toURI().toURL());
            } catch (MalformedURLException impossible) {
                throw new Error(impossible);
            }
        }
    }
}