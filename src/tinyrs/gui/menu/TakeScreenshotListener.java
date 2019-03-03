package tinyrs.gui.menu;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import tinyrs.Application;
import tinyrs.GlobalProperty;
import tinyrs.gui.PopupBuilder;

public final class TakeScreenshotListener implements ActionListener {

    private static final long START_DELAY_MILLIS = 250;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.YYYY.HHmm.ss");
    private final Component target;
    private final Icon popupIcon;
    private final Robot robot;

    public TakeScreenshotListener(final Component target, final Icon popupIcon, final Robot robot) {
        this.target = target;
        this.popupIcon = popupIcon;
        this.robot = robot;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(START_DELAY_MILLIS);
                final String screenshotFormat = GlobalProperty.SCREENSHOT_FORMAT.get();
                final File screenshotDirectory = new File(Application.storageDirectory(), "Screenshots");
                final File screenshotFile = new File(screenshotDirectory, DATE_FORMAT.format(new Date()) + '.' + screenshotFormat);
                final Rectangle visibleArea = target.getGraphicsConfiguration().getBounds().intersection(new Rectangle(
                        target.getLocationOnScreen(),
                        target.getSize()));
                final BufferedImage screenshot = robot.createScreenCapture(visibleArea);
                if (!ImageIO.write(screenshot, screenshotFormat, screenshotFile)) {
                    GlobalProperty.SCREENSHOT_FORMAT.setDefault();
                    throw new Exception();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (final Exception e) {
                    new PopupBuilder()
                            .withParent(target)
                            .withMessage("Could not take a screenshot.")
                            .withTitle("Screenshot Error")
                            .withMessageType(JOptionPane.WARNING_MESSAGE)
                            .withIcon(popupIcon)
                            .showMessage();
                }
            }
        }.execute();
    }
}
