package me.mtus.tinyrs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class Application {

    static final Properties properties;
    static {
        properties = new Properties();
        properties.setProperty("screenshotFormat", "png");
        properties.setProperty("confirmClose", "false");
        properties.setProperty("alwaysOnTop", "false");
        setDefaultWorldToTwo();
    }

    public static void main(String[] arguments) {
        String defaultWorld = null;
        File storageDirectory = null;
        for (String argument : arguments) {
            if (argument.startsWith("defaultWorld=")) {
                defaultWorld = argument.substring(13);
                if (!GameHelpers.isValidWorld(defaultWorld)) {
                    System.err.println("Invalid game world specified. Ignoring argument...");
                    defaultWorld = null;
                }
            } else if (argument.startsWith("storageDirectory=")) {
                File specifiedDirectory = new File(argument.substring(17));
                if (!specifiedDirectory.exists()) {
                    try {
                        RunnableFuture<Integer> promptTask = new FutureTask<Integer>(new Callable<Integer>() {

                            @Override
                            public Integer call() throws Exception {
                                return JOptionPane.showOptionDialog(null,
                                        "The storage directory you specified does not exist. Do you want to create it?",
                                        "Directory Missing",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null, null, null);
                            }
                        });
                        EventQueue.invokeAndWait(promptTask);
                        if (promptTask.get() != JOptionPane.YES_OPTION) {
                            continue;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (!specifiedDirectory.mkdirs()) {
                        showMessage("Could not create the specified directory.",
                                "Directory Error",
                                JOptionPane.WARNING_MESSAGE);
                        continue;
                    }
                }
                if (!specifiedDirectory.canRead() || !specifiedDirectory.canWrite()) {
                    showMessage("You do not have permission to read or write in the specified storage directory.",
                            "Missing Permissions",
                            JOptionPane.INFORMATION_MESSAGE);
                    continue;
                }
                storageDirectory = specifiedDirectory;
            }
        }
        boolean loadedProperties = false;
        if (storageDirectory == null) {
            storageDirectory = new File(System.getProperty("user.home"), "tinyrs");
            if ((storageDirectory.exists() || storageDirectory.mkdirs())
                    && storageDirectory.canRead()
                    && storageDirectory.canWrite()) {
                File propertiesFile = new File(storageDirectory, "tinyrs.properties");
                if (propertiesFile.exists()) {
                    try {
                        properties.load(new FileInputStream(propertiesFile));
                        loadedProperties = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Could not load the existing properties file.",
                                "Load Error",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            } else {
                storageDirectory = null;
                showMessage("Could not create a readable and writable storage directory in the user home folder.",
                        "Directory Error",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
        if (defaultWorld != null) {
            properties.setProperty("defaultWorld", defaultWorld);
        } else if (loadedProperties && !GameHelpers.isValidWorld(properties.getProperty("defaultWorld"))) {
            setDefaultWorldToTwo();
            System.err.println("Invalid game world specified in the properties file. Defaulting to " + properties.getProperty("defaultWorld") + "...");
        }
        if (storageDirectory != null) {
            properties.setProperty("storageDirectory", storageDirectory.getAbsolutePath());
        }
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                GameWindow window = new GameWindow();
                window.setTitle("tinyrs");
                window.setVisible(true);
            }
        });
    }

    static void saveProperties() {
        String storageDirectory = (String) properties.remove("storageDirectory");
        if (storageDirectory != null) {
            try {
                properties.store(new FileOutputStream(new File(storageDirectory, "tinyrs.properties")), null);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                properties.setProperty("storageDirectory", storageDirectory);
            }
        }
    }

    private static void setDefaultWorldToTwo() {
        properties.setProperty("defaultWorld", "2");
    }

    private static void showMessage(final String message, final String title, final int type) {
        try {
            EventQueue.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    JOptionPane.showMessageDialog(null, message, title, type);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}