package me.mtus.tinyrs;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

public class Application {

    static final Properties properties;
    static {
        properties = new Properties();
        properties.setProperty("defaultWorld", "2");
        properties.setProperty("confirmClose", "false");
    }

    public static void main(String[] arguments) {
        File storageDirectory = null;
        for (String argument : arguments) {
            if (argument.startsWith("defaultWorld=")) {
                String worldNumber = argument.substring(13);
                try {
                    InetAddress.getByName("oldschool" + Integer.parseInt(worldNumber) + ".runescape.com");
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                properties.setProperty("defaultWorld", worldNumber);
            } else if (argument.startsWith("storageDirectory=")) {
                File specifiedDirectory = new File(argument.substring(17));
                if (!specifiedDirectory.exists()) {
                    int userDecision = JOptionPane.showOptionDialog(null,
                            "The storage directory you specified does not exist. Do you want to create it?",
                            "Directory Missing",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, null, null);
                    if (userDecision == JOptionPane.YES_OPTION) {
                        if (!specifiedDirectory.mkdirs()) {
                            showMessage("Could not create the specified directory.",
                                    "Directory Error",
                                    JOptionPane.WARNING_MESSAGE);
                            continue;
                        }
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
        if (storageDirectory == null) {
            storageDirectory = new File(System.getProperty("user.home"), "tinyrs");
            if ((storageDirectory.exists() || storageDirectory.mkdirs())
                    && storageDirectory.canRead()
                    && storageDirectory.canWrite()) {
                File propertiesFile = new File(storageDirectory, "tinyrs.properties");
                if (propertiesFile.exists()) {
                    try {
                        properties.load(new FileInputStream(propertiesFile));
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
        if (storageDirectory != null) properties.setProperty("storageDirectory", storageDirectory.getAbsolutePath());
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
            }
            properties.setProperty("storageDirectory", storageDirectory);
        }
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