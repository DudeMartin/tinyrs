package tinyrs;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import tinyrs.utils.AppletUtility;

public class Application {

    static File storageDirectory;

    public static void main(String[] arguments) {
        String defaultWorld = null;
        File storageDirectory = null;
        for (String argument : arguments) {
            if (argument.startsWith("defaultWorld=")) {
                defaultWorld = argument.substring(13);
                if (!AppletUtility.isValidWorld(Integer.parseInt(defaultWorld))) {
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
                        GlobalProperty.read(new FileInputStream(propertiesFile));
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
    }

    static void saveProperties() {
        if (storageDirectory != null) {
            try {
                GlobalProperty.write(new FileOutputStream(new File(storageDirectory, "tinyrs.properties")));
            } catch (IOException e) {
                e.printStackTrace();
            }
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