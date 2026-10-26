package finalPPKG.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

/** The application's colors, typography and shared Swing defaults. */
public final class Theme {
    public static final Color BACKGROUND = new Color(0xF7F2E9);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_SOFT = new Color(0xFCF9F4);
    public static final Color TEXT = new Color(0x34251F);
    public static final Color MUTED = new Color(0x7E7066);
    public static final Color BORDER = new Color(0xE8DFD2);
    public static final Color PRIMARY = new Color(0xBE392A);
    public static final Color PRIMARY_SOFT = new Color(0xFBECE6);
    public static final Color SIDEBAR = new Color(0x702A22);
    public static final Color ON_DARK = new Color(0xFFF4E2);
    public static final Color ON_DARK_MUTED = new Color(0xE4BFAE);
    public static final Color ACCENT = new Color(0xF2C66D);
    public static final Color SUMMARY = new Color(0xFFF0D5);
    public static final Color SUCCESS = new Color(0x3C6B4F);
    public static final Color DANGER = new Color(0xAA3529);
    public static final Color SELECTION = new Color(0xFBE7DB);
    public static final int GAP = 16;
    public static final int CONTROL_HEIGHT = 44;
    public static final int CARD_RADIUS = 24;

    private Theme() {
    }

    public static Font font(int size, int style) {
        return new Font("Segoe UI", style, size);
    }

    /** Call once before building windows so all standard controls share a style. */
    public static void install() {
        UIDefaults defaults = UIManager.getDefaults();
        FontUIResource standard = new FontUIResource(font(14, Font.PLAIN));
        for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            if (defaults.get(key) instanceof Font) {
                defaults.put(key, standard);
            }
        }
        String[] controls = {"Label", "Button", "TextField", "FormattedTextField", "TextArea",
                "ComboBox", "Spinner", "List", "Table", "CheckBox", "RadioButton"};
        for (String control : controls) {
            UIManager.put(control + ".foreground", TEXT);
        }
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", BACKGROUND);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ToolTip.background", SIDEBAR);
        UIManager.put("ToolTip.foreground", SURFACE);
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder(6, 9, 6, 9));
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextField.selectionBackground", SELECTION);
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TextField.inactiveBackground", BACKGROUND);
        UIManager.put("TextField.border", inputBorder());
        UIManager.put("FormattedTextField.background", SURFACE);
        UIManager.put("FormattedTextField.caretForeground", TEXT);
        UIManager.put("FormattedTextField.selectionBackground", SELECTION);
        UIManager.put("FormattedTextField.selectionForeground", TEXT);
        UIManager.put("FormattedTextField.border", inputBorder());
        UIManager.put("TextArea.background", SURFACE);
        UIManager.put("TextArea.selectionBackground", SELECTION);
        UIManager.put("TextArea.selectionForeground", TEXT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.selectionBackground", SELECTION);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ComboBox.border", inputBorder());
        UIManager.put("ComboBox.buttonBackground", SURFACE_SOFT);
        UIManager.put("ComboBox.disabledBackground", SURFACE_SOFT);
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("Spinner.background", SURFACE);
        UIManager.put("Spinner.border", inputBorder());
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.selectionBackground", SELECTION);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", SURFACE_SOFT);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.background", BACKGROUND);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.thumbDarkShadow", BORDER);
        UIManager.put("ScrollBar.thumbShadow", BORDER);
        UIManager.put("ScrollBar.thumbHighlight", BORDER);
        UIManager.put("CheckBox.focus", PRIMARY);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("Button.font", new FontUIResource(font(14, Font.BOLD)));
    }

    /** Flat, readable controls without the platform's inset bevels. */
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                return arrowButton(javax.swing.SwingConstants.SOUTH);
            }
        });
        combo.setBorder(inputBorder());
        combo.setBackground(SURFACE);
        combo.setFocusable(true);
        combo.setMaximumRowCount(10);
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setUI(new BasicSpinnerUI() {
            @Override protected Component createNextButton() {
                JButton button = arrowButton(javax.swing.SwingConstants.NORTH);
                installNextButtonListeners(button);
                return button;
            }
            @Override protected Component createPreviousButton() {
                JButton button = arrowButton(javax.swing.SwingConstants.SOUTH);
                installPreviousButtonListeners(button);
                return button;
            }
        });
        spinner.setBorder(inputBorder());
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
            editor.setOpaque(false);
            editor.getTextField().setBorder(BorderFactory.createEmptyBorder());
            editor.getTextField().setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            editor.getTextField().addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent event) { spinner.repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent event) { spinner.repaint(); }
            });
        }
    }

    private static JButton arrowButton(int direction) {
        BasicArrowButton button = new BasicArrowButton(direction, SURFACE_SOFT, SURFACE_SOFT, MUTED, SURFACE_SOFT) {
            @Override public void paint(Graphics graphics) {
                graphics.setColor(getModel().isPressed() ? SELECTION : SURFACE_SOFT);
                graphics.fillRect(0, 0, getWidth(), getHeight());
                paintTriangle(graphics, (getWidth() - 7) / 2, (getHeight() - 4) / 2, 4, getDirection(), isEnabled());
            }
        };
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFocusable(false);
        button.setPreferredSize(new java.awt.Dimension(24, 18));
        return button;
    }

    private static javax.swing.border.Border inputBorder() {
        return new InputBorder();
    }

    private static final class InputBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component component) { return new Insets(7, 12, 7, 12); }
        @Override public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(7, 12, 7, 12);
            return insets;
        }
        @Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean focused = component.hasFocus() || focus != null
                    && component instanceof JComponent control && SwingUtilities.isDescendingFrom(focus, control);
            canvas.setColor(focused ? PRIMARY : BORDER);
            canvas.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            canvas.dispose();
        }
    }
}
