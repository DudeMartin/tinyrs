package me.mtus.tinyrs;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

class GameWindow extends JFrame {

    GameWindow() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        final String storageDirectory = Application.properties.getProperty("storageDirectory");
        if (storageDirectory != null) {
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
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });
            fileMenu.add(openDirectoryItem);
        }
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
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
        setSize(800, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent windowEvent) {
               boolean close = true;
               if (Boolean.valueOf(Application.properties.getProperty("confirmClose", "false")) == Boolean.TRUE) {
                   close = JOptionPane.showOptionDialog(GameWindow.this,
                           "Are you sure you want to close?",
                           "Confirm Close",
                           JOptionPane.YES_NO_OPTION,
                           JOptionPane.QUESTION_MESSAGE,
                           null, null, null) == JOptionPane.YES_OPTION;
               }
               if (close) {
                   Application.saveProperties();
                   System.exit(0);
               }
            }
        });
    }

    private static ImageIcon loadIcon(String name) {
        return new ImageIcon(GameWindow.class.getResource("/resources/" + name));
    }
}