package finalPPKG.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.BorderFactory;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/** The application's colors, typography and shared Swing defaults. */
public final class Theme {
    public static final Color BACKGROUND = new Color(0xF5F4EF);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(0x23352C);
    public static final Color MUTED = new Color(0x718078);
    public static final Color BORDER = new Color(0xE4E7DF);
    public static final Color PRIMARY = new Color(0xC35C37);
    public static final Color SIDEBAR = new Color(0x202824);
    public static final Color SUCCESS = new Color(0x407957);
    public static final Color DANGER = new Color(0xB5493E);
    public static final Color SELECTION = new Color(0xF6E8DF);

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
        UIManager.put("Spinner.background", SURFACE);
        UIManager.put("Spinner.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.selectionBackground", SELECTION);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("Button.font", new FontUIResource(font(14, Font.BOLD)));
    }

    private static javax.swing.border.Border inputBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }
}
