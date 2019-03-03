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

public class Application {

    private static final int INVALID_WORLD = -1;
    private static File storageDirectory;

    public static void main(final String[] arguments) {
        File storageDirectory = null;
        int defaultWorld = INVALID_WORLD;
        for (final String argument : arguments) {
            if (argument.startsWith("storageDirectory=")) {
                final File specifiedDirectory = new File(argument.substring(17));
                if (!specifiedDirectory.exists()) {
                    final int createOption = new PopupBuilder()
                            .withMessage("The storage directory you specified does not exist. Do you want to create it?")
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
                defaultWorld = parseValidWorld(argument.substring(13));
                if (defaultWorld == INVALID_WORLD) {
                    System.err.println("The specified default world is invalid. Ignoring argument...");
                }
            }
        }
        boolean loadedProperties = false;
        if (storageDirectory == null) {
            storageDirectory = new File(System.getProperty("user.home"), "tinyrs");
            if ((storageDirectory.exists() || storageDirectory.mkdirs())
                    && storageDirectory.canRead()
                    && storageDirectory.canWrite()) {
                Application.storageDirectory = storageDirectory;
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
        if (defaultWorld != INVALID_WORLD) {
            GlobalProperty.DEFAULT_WORLD.set(defaultWorld);
        } else if (loadedProperties && !AppletUtility.isValidWorld(GlobalProperty.DEFAULT_WORLD.get(int.class))) {
            GlobalProperty.DEFAULT_WORLD.setDefault();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (Application.storageDirectory != null) {
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

    public static File storageDirectory() {
        return storageDirectory;
    }

    private static int parseValidWorld(final String world) {
        final int worldNumber;
        try {
            worldNumber = Integer.parseInt(world);
        } catch (NumberFormatException expected) {
            return INVALID_WORLD;
        }
        if (!AppletUtility.isValidWorld(worldNumber)) {
            return INVALID_WORLD;
        }
        return worldNumber;
    }
}