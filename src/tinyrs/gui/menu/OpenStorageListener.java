package tinyrs.gui.menu;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import tinyrs.Application;
import tinyrs.gui.PopupBuilder;

public final class OpenStorageListener implements ActionListener {

    private final Component component;
    private final Icon popupIcon;

    public OpenStorageListener(final Component component, final Icon popupIcon) {
        this.component = component;
        this.popupIcon = popupIcon;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        try {
            Desktop.getDesktop().open(Application.storageDirectory());
        } catch (final Exception exception) {
            exception.printStackTrace();
            new PopupBuilder()
                    .withParent(component)
                    .withMessage("Could not open the storage directory.")
                    .withTitle("Storage Error")
                    .withMessageType(JOptionPane.WARNING_MESSAGE)
                    .withIcon(popupIcon)
                    .showMessage();
        }
    }
}
