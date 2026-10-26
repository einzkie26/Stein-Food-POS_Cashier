package finalPPKG.ui.components;

import finalPPKG.model.MenuItem;
import finalPPKG.ui.theme.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JToggleButton;
import javax.swing.Timer;

/** A keyboard-accessible dish card with short, event-driven hover and selection transitions. */
public final class MenuCard extends JToggleButton {
    public static final int HEIGHT = 68;
    private final MenuItem item;
    private final Timer animation;
    private float hover;
    private float selection;
    private float fromHover;
    private float fromSelection;
    private float targetHover;
    private float targetSelection;
    private long started;

    public MenuCard(MenuItem item) {
        super(item.getName());
        this.item = item;
        setIcon(MenuIcons.dish(item, 32));
        setFont(Theme.font(12, Font.BOLD));
        setPreferredSize(new Dimension(168, HEIGHT));
        setMinimumSize(new Dimension(130, HEIGHT));
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setRolloverEnabled(true);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(item.getName() + " · " + item.getCategory());
        getAccessibleContext().setAccessibleName(item.getName() + ", " + item.getCategory());
        getAccessibleContext().setAccessibleDescription("Select this dish, then enter quantity and unit price.");
        animation = new Timer(15, event -> advanceAnimation());
        animation.setCoalesce(true);
        getModel().addChangeListener(event -> animateToState());
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { repaint(); }
            @Override public void focusLost(FocusEvent event) { repaint(); }
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) { finishAnimation(); }
        });
    }

    public MenuItem getItem() { return item; }

    private void animateToState() {
        float nextHover = isEnabled() && getModel().isRollover() ? 1 : 0;
        float nextSelection = isSelected() ? 1 : 0;
        if (nextHover == targetHover && nextSelection == targetSelection) { return; }
        targetHover = nextHover;
        targetSelection = nextSelection;
        if (!isShowing() || Boolean.getBoolean("stein.ui.reduceMotion")) {
            finishAnimation();
            return;
        }
        fromHover = hover;
        fromSelection = selection;
        started = System.nanoTime();
        animation.restart();
    }

    private void advanceAnimation() {
        float progress = Math.min(1, (System.nanoTime() - started) / 170_000_000f);
        float eased = 1 - (float) Math.pow(1 - progress, 3);
        hover = fromHover + (targetHover - fromHover) * eased;
        selection = fromSelection + (targetSelection - fromSelection) * eased;
        repaint();
        if (progress >= 1) { animation.stop(); }
    }

    private void finishAnimation() {
        animation.stop();
        hover = targetHover;
        selection = targetSelection;
        repaint();
    }

    @Override public void removeNotify() {
        finishAnimation();
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int y = Math.round(3 - hover * 2);
        int width = getWidth() - 3;
        int height = getHeight() - 6;
        canvas.setColor(new Color(16, 45, 74, Math.round(8 + 10 * hover)));
        canvas.fillRoundRect(1, y + 2, width, height, 14, 14);
        Color surface = blend(Theme.SURFACE, Theme.PRIMARY_SOFT, Math.max(selection, hover * 0.55f));
        if (getModel().isPressed()) { surface = Theme.SELECTION; }
        canvas.setColor(surface);
        canvas.fillRoundRect(1, y, width, height, 14, 14);
        canvas.setStroke(new BasicStroke(selection > 0 ? 1.5f : 1));
        canvas.setColor(blend(Theme.BORDER, Theme.PRIMARY, Math.max(selection, hover * 0.7f)));
        canvas.drawRoundRect(1, y, width, height, 14, 14);
        if (hasFocus()) {
            canvas.setColor(Theme.PRIMARY);
            canvas.setStroke(new BasicStroke(2));
            canvas.drawRoundRect(3, y + 2, width - 4, height - 4, 11, 11);
        }
        getIcon().paintIcon(this, canvas, 11, y + (height - 32) / 2);
        int textX = 53;
        int textWidth = Math.max(30, getWidth() - textX - 20);
        canvas.setFont(getFont());
        canvas.setColor(isEnabled() ? Theme.TEXT : Theme.MUTED);
        FontMetrics metrics = canvas.getFontMetrics();
        List<String> lines = wrap(item.getName(), metrics, textWidth);
        int baseline = y + (lines.size() == 1 ? 26 : 19);
        for (String line : lines) {
            canvas.drawString(line, textX, baseline);
            baseline += metrics.getHeight();
        }
        canvas.setFont(Theme.font(10, Font.PLAIN));
        canvas.setColor(Theme.MUTED);
        canvas.drawString(item.getCategory(), textX, y + height - 9);
        if (selection > 0) {
            canvas.setColor(new Color(Theme.PRIMARY.getRed(), Theme.PRIMARY.getGreen(),
                    Theme.PRIMARY.getBlue(), Math.round(255 * selection)));
            int checkX = getWidth() - 17;
            canvas.fillOval(checkX, y + 7, 11, 11);
            canvas.setColor(Theme.SURFACE);
            canvas.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            canvas.drawLine(checkX + 3, y + 12, checkX + 5, y + 14);
            canvas.drawLine(checkX + 5, y + 14, checkX + 8, y + 10);
        }
        canvas.dispose();
    }

    private static List<String> wrap(String text, FontMetrics metrics, int width) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && metrics.stringWidth(candidate) > width) {
                lines.add(line);
                line = word;
            } else { line = candidate; }
        }
        lines.add(line);
        if (lines.size() > 2) {
            String remainder = String.join(" ", lines.subList(1, lines.size()));
            while (!remainder.isEmpty() && metrics.stringWidth(remainder + "…") > width) {
                remainder = remainder.substring(0, remainder.length() - 1);
            }
            return List.of(lines.get(0), remainder + "…");
        }
        return lines;
    }

    private static Color blend(Color from, Color to, float fraction) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * fraction),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * fraction),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * fraction));
    }
}
