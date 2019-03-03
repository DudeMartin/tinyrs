package tinyrs;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import tinyrs.gui.GameWindow;
import tinyrs.gui.PopupBuilder;
import tinyrs.utils.AppletUtility;

public final class Application {

    private static final int INVALID_WORLD = -1;
    private static File storageDirectory;

    public static void main(final String[] arguments) {
        for (final String argument : arguments) {
            if (argument.startsWith("storageDirectory=")) {
                final File specifiedDirectory = new File(argument.substring(17));
                if (!specifiedDirectory.exists()) {
                    final int createOption = new PopupBuilder()
                            .withMessage("The specified storage directory does not exist. Do you want to create it?")
                            .withTitle("Storage Error")
                            .withMessageType(JOptionPane.QUESTION_MESSAGE)
                            .showYesNoInput();
                    if (createOption != JOptionPane.YES_OPTION) {
                        continue;
                    }
                    if (!specifiedDirectory.mkdirs()) {
                        new PopupBuilder()
                                .withMessage("Could not create the specified directory.")
                                .withTitle("Storage Error")
                                .withMessageType(JOptionPane.WARNING_MESSAGE)
                                .showMessage();
                        continue;
                    }
                }
                if (!specifiedDirectory.canRead() || !specifiedDirectory.canWrite()) {
                    new PopupBuilder()
                            .withMessage("You do not have permission to read or write in the specified storage directory.")
                            .withTitle("Storage Error")
                            .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .showMessage();
                    continue;
                }
                storageDirectory = specifiedDirectory;
            } else if (argument.startsWith("defaultWorld=")) {
                final int defaultWorld = parseValidWorld(argument.substring(13));
                if (defaultWorld == INVALID_WORLD) {
                    System.err.println("The specified default world is invalid. Ignoring argument...");
                } else {
                    GlobalProperty.DEFAULT_WORLD.set(defaultWorld);
                }
            }
        }
        boolean loadedProperties = false;
        if (storageDirectory == null) {
            storageDirectory = new File(System.getProperty("user.home"), "tinyrs");
            if (isStorageDirectoryAvailable() || storageDirectory.mkdirs() && isStorageDirectoryAvailable()) {
                final File propertiesFile = new File(storageDirectory, "tinyrs.properties");
                if (propertiesFile.exists()) {
                    try {
                        GlobalProperty.read(new FileInputStream(propertiesFile));
                        loadedProperties = true;
                    } catch (final IOException e) {
                        e.printStackTrace();
                        new PopupBuilder()
                                .withMessage("Could not read properties from the existing file.")
                                .withTitle("Properties Error")
                                .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                                .showMessage();
                    }
                }
            } else {
                new PopupBuilder()
                        .withMessage("Could not create a readable and writable storage directory in the user's home folder.")
                        .withTitle("Storage Error")
                        .withMessageType(JOptionPane.WARNING_MESSAGE)
                        .showMessage();
            }
        }
        if (loadedProperties && !AppletUtility.isValidWorld(GlobalProperty.DEFAULT_WORLD.get(int.class))) {
            GlobalProperty.DEFAULT_WORLD.setDefault();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (Application.isStorageDirectoryAvailable()) {
                    try {
                        GlobalProperty.write(new FileOutputStream(
                                new File(Application.storageDirectory, "tinyrs.properties")));
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                GameWindow window = new GameWindow();
                window.setTitle("tinyrs");
                window.setVisible(true);
                window.pack();
            }
        });
    }

    public static boolean isStorageDirectoryAvailable() {
        return storageDirectory != null && storageDirectory.canRead() && storageDirectory.canWrite();
    }

    public static File storageDirectory() {
        return storageDirectory;
    }

    private static int parseValidWorld(final String world) {
        final int worldNumber;
        try {
            worldNumber = Integer.parseInt(world);
        } catch (final NumberFormatException expected) {
            return INVALID_WORLD;
        }
        if (!AppletUtility.isValidWorld(worldNumber)) {
            return INVALID_WORLD;
        }
        return worldNumber;
    }
}
