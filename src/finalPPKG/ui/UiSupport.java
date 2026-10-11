package finalPPKG.ui;

import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Shared layout and formatting helpers; screen behavior lives in the views. */
public final class UiSupport {
    private UiSupport() { }

    public static JLabel label(String text, int size, int style, java.awt.Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(size, style));
        label.setForeground(color);
        return label;
    }

    public static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JPanel field(String name, JComponent control) {
        JPanel panel = transparent(new BorderLayout(0, 7));
        JLabel label = label(name, 12, Font.BOLD, Theme.TEXT);
        label.setLabelFor(control);
        panel.add(label, BorderLayout.NORTH);
        panel.add(control, BorderLayout.CENTER);
        control.getAccessibleContext().setAccessibleName(name);
        return panel;
    }

    public static String money(BigDecimal amount) {
        return "PHP " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(amount);
    }

    public static void padding(JComponent component, int value) {
        component.setBorder(BorderFactory.createEmptyBorder(value, value, value, value));
    }

    public static JPanel pair(Component left, Component right) {
        JPanel panel = transparent(new BorderLayout(12, 0));
        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }
}
