package tinyrs;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import tinyrs.gui.PopupBuilder;
import tinyrs.utils.AppletUtility;

public class Application {

    static File storageDirectory;

    public static void main(String[] arguments) {
        String defaultWorld = null;
        File storageDirectory = null;
        for (String argument : arguments) {
            if (argument.startsWith("defaultWorld=")) {
                defaultWorld = argument.substring(13);
                if (!AppletUtility.isValidWorld(defaultWorld)) {
                    System.err.println("Invalid game world specified. Ignoring argument...");
                    defaultWorld = null;
                }
            } else if (argument.startsWith("storageDirectory=")) {
                File specifiedDirectory = new File(argument.substring(17));
                if (!specifiedDirectory.exists()) {
                    int createOption = new PopupBuilder()
                            .withMessage("The storage directory you specified does not exist. Do you want to create it?")
                            .withTitle("Directory Missing")
                            .withMessageType(JOptionPane.QUESTION_MESSAGE)
                            .showYesNoInput();
                    if (createOption != JOptionPane.YES_OPTION) {
                        continue;
                    }
                    if (!specifiedDirectory.mkdirs()) {
                        new PopupBuilder()
                                .withMessage("Could not create the specified directory.")
                                .withTitle("Directory Error")
                                .withMessageType(JOptionPane.WARNING_MESSAGE)
                                .showMessage();
                        continue;
                    }
                }
                if (!specifiedDirectory.canRead() || !specifiedDirectory.canWrite()) {
                    new PopupBuilder()
                            .withMessage("You do not have permission to read or write in the specified storage directory.")
                            .withTitle("Missing Permissions")
                            .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .showMessage();
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
                        GlobalProperty.read(new FileInputStream(propertiesFile));
                        loadedProperties = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        new PopupBuilder()
                                .withMessage("Could not load the existing properties file.")
                                .withTitle("Load Error")
                                .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                                .showMessage();
                    }
                }
            } else {
                storageDirectory = null;
                new PopupBuilder()
                        .withMessage("Could not create a readable and writable storage directory in the user home folder.")
                        .withTitle("Directory Error")
                        .withMessageType(JOptionPane.WARNING_MESSAGE)
                        .showMessage();
            }
        }
        if (defaultWorld != null) {
            GlobalProperty.DEFAULT_WORLD.set(defaultWorld);
        } else if (loadedProperties && !AppletUtility.isValidWorld(GlobalProperty.DEFAULT_WORLD.get(int.class))) {
            GlobalProperty.DEFAULT_WORLD.setDefault();
            System.err.println("Invalid game world specified in the properties file. Defaulting to " + GlobalProperty.DEFAULT_WORLD.getDefault() + "...");
        }
        if (storageDirectory != null) {
            Application.storageDirectory = storageDirectory;
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
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                if (Application.storageDirectory != null) {
                    try {
                        GlobalProperty.write(new FileOutputStream(
                                new File(Application.storageDirectory, "tinyrs.properties")));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));
    }
}