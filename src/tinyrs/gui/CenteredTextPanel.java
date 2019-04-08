package tinyrs.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

final class CenteredTextPanel extends JPanel {

    private String text;
    private Component anchor;
    private int spaceAbove;
    private int textCenterX;
    private int textCenterY;

    CenteredTextPanel() {
        super(new GridBagLayout());
        setBackground(Color.BLACK);
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(final ComponentEvent e) {
                calculateCenter();
            }
        });
    }

    @Override
    public void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (text != null) {
            final FontMetrics metrics = g.getFontMetrics();
            g.setColor(Color.WHITE);
            g.drawString(text, textCenterX - metrics.stringWidth(text) / 2, textCenterY + metrics.getHeight() / 2);
        }
    }

    void showText(final String text) {
        this.text = text;
        this.anchor = null;
        calculateCenter();
    }

    void showTextAbove(final String text, final Component anchor, final int spaceAbove) {
        if (!SwingUtilities.isDescendingFrom(anchor, this)) {
            throw new IllegalArgumentException("The anchor must be within this panel.");
        }
        this.text = text;
        this.anchor = anchor;
        this.spaceAbove = spaceAbove;
        calculateCenter();
    }

    private void calculateCenter() {
        if (anchor == null || !SwingUtilities.isDescendingFrom(anchor, this)) {
            textCenterX = getX() + getWidth() / 2;
            textCenterY = getY() + getHeight() / 2;
        } else {
            textCenterX = anchor.getX() + anchor.getWidth() / 2;
            textCenterY = anchor.getY() - spaceAbove;
        }
        repaint();
    }
}