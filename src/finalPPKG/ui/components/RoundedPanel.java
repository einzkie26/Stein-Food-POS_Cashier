package finalPPKG.ui.components;

import finalPPKG.ui.theme.Theme;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/** Reusable card with a quiet border and a rounded surface. */
public class RoundedPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int CORNER_DIAMETER = Theme.CARD_RADIUS;

    public RoundedPanel(LayoutManager layout) {
        this(layout, Theme.SURFACE);
    }

    public RoundedPanel(LayoutManager layout, Color background) {
        super(layout);
        setOpaque(false);
        setBackground(background);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(getBackground());
        canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_DIAMETER, CORNER_DIAMETER);
        canvas.setColor(Theme.BORDER);
        canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_DIAMETER, CORNER_DIAMETER);
        canvas.dispose();
        super.paintComponent(graphics);
    }
}
