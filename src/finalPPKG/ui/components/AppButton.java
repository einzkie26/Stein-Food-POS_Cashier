package finalPPKG.ui.components;

import finalPPKG.ui.theme.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;

/** Consistent buttons for actions and sidebar navigation. */
public class AppButton extends JButton {
    private static final long serialVersionUID = 1L;

    public enum Style { PRIMARY, SECONDARY, DANGER, NAVIGATION }

    private final Style style;
    private boolean active;

    public AppButton(String text, Style style) {
        super(text);
        this.style = style;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setFont(Theme.font(14, Font.BOLD));
        setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(12);
        updateForeground();
        getModel().addChangeListener(event -> repaint());
    }

    public void setActive(boolean active) {
        this.active = active;
        updateForeground();
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    private void updateForeground() {
        setForeground(style == Style.SECONDARY ? Theme.TEXT
                : style == Style.NAVIGATION && !active ? new Color(0xC0CDC5) : Theme.SURFACE);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color background = backgroundColor();
        if (!isEnabled()) {
            background = style == Style.NAVIGATION ? Theme.SIDEBAR : Theme.BORDER;
        } else if (getModel().isPressed()) {
            background = shade(background, 0.90);
        } else if (getModel().isRollover()) {
            background = style == Style.NAVIGATION ? new Color(0x35423A)
                    : style == Style.SECONDARY ? new Color(0xF2F3ED) : shade(background, 0.94);
        }
        canvas.setColor(background);
        canvas.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 18, 18);
        if (style == Style.SECONDARY) {
            canvas.setColor(Theme.BORDER);
            canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 18, 18);
        }
        if (hasFocus()) {
            canvas.setStroke(new BasicStroke(2));
            canvas.setColor(style == Style.NAVIGATION ? new Color(0x92A99B) : Theme.PRIMARY);
            canvas.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 20, 20);
        }
        canvas.dispose();
        super.paintComponent(graphics);
    }

    private Color backgroundColor() {
        switch (style) {
            case PRIMARY: return Theme.PRIMARY;
            case SECONDARY: return Theme.SURFACE;
            case DANGER: return Theme.DANGER;
            case NAVIGATION: return active ? new Color(0x3E5144) : Theme.SIDEBAR;
            default: return Theme.SURFACE;
        }
    }

    private static Color shade(Color color, double amount) {
        return new Color((int) (color.getRed() * amount), (int) (color.getGreen() * amount),
                (int) (color.getBlue() * amount), color.getAlpha());
    }
}
