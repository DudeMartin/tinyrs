package tinyrs.gui.menu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import tinyrs.GlobalProperty;
import tinyrs.gui.PopupBuilder;
import tinyrs.utils.AppletUtility;

public final class SetDefaultWorldListener implements ActionListener {

    private final Component component;
    private final Icon popupIcon;

    public SetDefaultWorldListener(final Component component, final Icon popupIcon) {
        this.component = component;
        this.popupIcon = popupIcon;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        final String worldInput = new PopupBuilder()
                .withParent(component)
                .withMessage("Please enter a world number.")
                .withTitle("Enter World")
                .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                .withIcon(popupIcon)
                .showTextInput();
        if (worldInput != null && !worldInput.isEmpty()) {
            final int world;
            try {
                world = Integer.parseInt(worldInput);
            } catch (final NumberFormatException expected) {
                new PopupBuilder()
                        .withParent(component)
                        .withMessage("Please enter a positive integer.")
                        .withTitle("Input Error")
                        .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .withIcon(popupIcon)
                        .showMessage();
                return;
            }
            if (AppletUtility.isValidWorld(world)) {
                GlobalProperty.DEFAULT_WORLD.set(world);
            } else {
                new PopupBuilder()
                        .withParent(component)
                        .withMessage("This world is unreachable or does not exist.")
                        .withTitle("Input Error")
                        .withMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .withIcon(popupIcon)
                        .showMessage();
            }
        }
    }
}
