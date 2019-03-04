package tinyrs.plugin;

import java.applet.Applet;
import javax.swing.JMenuItem;

public interface Plugin {

    void start(Applet gameApplet);

    void stop();

    JMenuItem createMenuItem();
}
