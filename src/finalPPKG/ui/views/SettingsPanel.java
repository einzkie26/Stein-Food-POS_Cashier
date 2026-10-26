package finalPPKG.ui.views;

import finalPPKG.app.AppConfig;
import finalPPKG.app.OrderActions;
import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class SettingsPanel extends WorkspacePanel {
    public SettingsPanel(AppConfig config, OrderActions actions) {
        super("Settings");
        JPanel stack = UiSupport.transparent(new java.awt.GridBagLayout());
        RoundedPanel appearance = new RoundedPanel(new BorderLayout(0, 12));
        UiSupport.padding(appearance, 24);
        appearance.add(UiSupport.label("Workspace", 17, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        JCheckBox compact = new JCheckBox("Use compact table rows");
        compact.setOpaque(false);
        compact.setForeground(Theme.TEXT);
        compact.addActionListener(event -> actions.setCompactRows(compact.isSelected()));
        appearance.add(compact, BorderLayout.CENTER);
        appearance.add(UiSupport.label("This preference applies for the current session.", 12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        RoundedPanel files = new RoundedPanel(new BorderLayout(0, 16));
        UiSupport.padding(files, 24);
        files.add(UiSupport.label("Saved files", 17, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        JPanel paths = UiSupport.transparent(new GridLayout(2, 1, 0, 14));
        paths.add(pathField("Order history", config.getOrderFile().toString()));
        paths.add(pathField("Receipt history", config.getReceiptHistoryDirectory().toString()));
        files.add(paths, BorderLayout.CENTER);
        AppButton refresh = new AppButton("Reload order history", AppButton.Style.SECONDARY);
        refresh.setIcon(new AppIcon(AppIcon.Kind.REFRESH, 16, Theme.MUTED));
        refresh.addActionListener(event -> actions.refreshOrders());
        JPanel buttonRow = UiSupport.transparent(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        buttonRow.add(refresh);
        files.add(buttonRow, BorderLayout.SOUTH);
        appearance.setPreferredSize(new java.awt.Dimension(0, 150));
        files.setPreferredSize(new java.awt.Dimension(0, 292));
        java.awt.GridBagConstraints constraints = new java.awt.GridBagConstraints();
        constraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new java.awt.Insets(0, 0, 18, 0);
        stack.add(appearance, constraints);
        constraints.gridy = 1;
        constraints.insets = new java.awt.Insets(0, 0, 0, 0);
        stack.add(files, constraints);
        body.add(stack, BorderLayout.NORTH);
    }

    private JPanel pathField(String name, String path) {
        JTextField field = new JTextField(path);
        field.setEditable(false);
        field.setPreferredSize(new java.awt.Dimension(0, Theme.CONTROL_HEIGHT));
        field.setCaretPosition(0);
        field.setToolTipText(path);
        return UiSupport.field(name, field);
    }

    @Override public void onOrdersChanged(List<Order> orders) { }
}
