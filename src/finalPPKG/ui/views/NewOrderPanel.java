package finalPPKG.ui.views;

import finalPPKG.app.OrderActions;
import finalPPKG.model.MenuItem;
import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Entry, live totals and the complete current order list share one screen. */
public final class NewOrderPanel extends WorkspacePanel {
    private final OrderFormPanel form;
    private final OrderListPanel orderList;
    private final AppButton save = new AppButton("Add to order", AppButton.Style.PRIMARY);
    private final JLabel subtotal = UiSupport.label("PHP 0.00", 14, Font.BOLD, Theme.TEXT);
    private final JLabel savings = UiSupport.label("PHP 0.00", 14, Font.BOLD, Theme.SUCCESS);
    private final JLabel total = UiSupport.label("PHP 0.00", 32, Font.BOLD, Theme.PRIMARY);
    private final JLabel feedback = UiSupport.label("", 12, Font.PLAIN, Theme.MUTED);
    private boolean storageReady;

    public NewOrderPanel(List<MenuItem> menu, OrderActions actions) {
        super("New order");
        form = new OrderFormPanel(menu, null);
        orderList = new OrderListPanel(menu, actions);
        RoundedPanel entry = new RoundedPanel(new BorderLayout(0, 10));
        UiSupport.padding(entry, 18);
        entry.add(UiSupport.pair(UiSupport.label("Order details", 17, Font.BOLD, Theme.TEXT),
                UiSupport.label("FRESHLY PREPARED", 10, Font.BOLD, Theme.PRIMARY)), BorderLayout.NORTH);
        entry.add(form, BorderLayout.CENTER);
        JPanel entryFooter = UiSupport.transparent(new BorderLayout());
        AppButton reset = new AppButton("Reset", AppButton.Style.SECONDARY);
        reset.addActionListener(event -> form.reset());
        save.setIcon(new AppIcon(AppIcon.Kind.PLUS, 16, Color.WHITE));
        save.setName("saveOrder");
        save.addActionListener(event -> {
            try {
                if (actions.addOrder(form.readOrder())) { form.reset(); }
            } catch (IllegalArgumentException exception) {
                feedback.setText(exception.getMessage());
                feedback.setForeground(Theme.DANGER);
            }
        });
        entryFooter.add(reset, BorderLayout.WEST);
        entryFooter.add(save, BorderLayout.EAST);
        entry.add(entryFooter, BorderLayout.SOUTH);

        RoundedPanel preview = new RoundedPanel(new BorderLayout(0, 14), Theme.SUMMARY);
        UiSupport.padding(preview, 18);
        preview.add(UiSupport.pair(UiSupport.label("Item summary", 17, Font.BOLD, Theme.TEXT),
                new JLabel(new AppIcon(AppIcon.Kind.RECEIPT, 22, Theme.PRIMARY))), BorderLayout.NORTH);
        JPanel breakdown = UiSupport.transparent(new GridLayout(2, 1, 0, 10));
        breakdown.add(UiSupport.pair(UiSupport.label("Subtotal", 13, Font.PLAIN, Theme.MUTED), subtotal));
        breakdown.add(UiSupport.pair(UiSupport.label("Discount", 13, Font.PLAIN, Theme.MUTED), savings));
        JPanel summary = UiSupport.transparent(new BorderLayout(0, 10));
        summary.add(breakdown, BorderLayout.NORTH);
        JPanel totalBlock = UiSupport.transparent(new BorderLayout(0, 6));
        totalBlock.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SUMMARY_BORDER),
                BorderFactory.createEmptyBorder(12, 0, 0, 0)));
        totalBlock.add(UiSupport.label("ITEM TOTAL", 11, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
        totalBlock.add(total, BorderLayout.CENTER);
        summary.add(totalBlock, BorderLayout.CENTER);
        preview.add(summary, BorderLayout.CENTER);
        feedback.setPreferredSize(new Dimension(0, 36));
        preview.add(feedback, BorderLayout.SOUTH);
        JPanel columns = UiSupport.transparent(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.gridy = 0;
        constraints.gridx = 0;
        constraints.weightx = 0.62;
        constraints.insets = new Insets(0, 0, 0, Theme.GAP);
        entry.setPreferredSize(new Dimension(0, 276));
        entry.setMinimumSize(new Dimension(0, 276));
        columns.add(entry, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.38;
        constraints.insets = new Insets(0, 0, 0, 0);
        preview.setPreferredSize(new Dimension(0, 276));
        preview.setMinimumSize(new Dimension(0, 276));
        columns.add(preview, constraints);
        columns.setPreferredSize(new Dimension(0, 276));
        body.add(columns, BorderLayout.NORTH);
        body.add(orderList, BorderLayout.CENTER);
        form.setChangeListener(this::refreshPreview);
        refreshPreview();
    }

    private void refreshPreview() {
        try {
            Order order = form.readOrder();
            subtotal.setText(UiSupport.money(order.getSubtotal()));
            savings.setText("− " + UiSupport.money(order.getDiscountAmount()));
            total.setText(UiSupport.money(order.getTotal()));
            feedback.setText(storageReady ? "Ready to add to your order." : "<html>Order history is unavailable.<br>Use Refresh to reload.</html>");
            feedback.setForeground(storageReady ? Theme.SUCCESS : Theme.MUTED);
            feedback.setToolTipText(null);
            save.setEnabled(storageReady);
        } catch (IllegalArgumentException exception) {
            subtotal.setText(UiSupport.money(BigDecimal.ZERO));
            savings.setText(UiSupport.money(BigDecimal.ZERO));
            total.setText(UiSupport.money(BigDecimal.ZERO));
            feedback.setText("<html>" + exception.getMessage().replace("&", "&amp;")
                    .replace("<", "&lt;").replace(">", "&gt;") + "</html>");
            feedback.setToolTipText(exception.getMessage());
            feedback.setForeground(Theme.MUTED);
            save.setEnabled(false);
        }
    }

    @Override public void onOrdersChanged(List<Order> orders) { orderList.setOrders(orders); }
    @Override public void setCompactRows(boolean compact) { orderList.setCompactRows(compact); }
    @Override public void setStorageReady(boolean ready) {
        storageReady = ready;
        orderList.setStorageReady(ready);
        refreshPreview();
    }
    @Override public Dimension getPreferredSize() {
        return new Dimension(super.getPreferredSize().width, 640);
    }
}

