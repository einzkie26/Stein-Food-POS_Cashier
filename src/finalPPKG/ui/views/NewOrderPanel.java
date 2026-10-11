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
    private final JLabel total = UiSupport.label("PHP 0.00", 30, Font.BOLD, Theme.TEXT);
    private final JLabel feedback = UiSupport.label("", 12, Font.PLAIN, Theme.MUTED);
    private boolean storageReady;

    public NewOrderPanel(List<MenuItem> menu, OrderActions actions) {
        super("New order");
        form = new OrderFormPanel(menu, null);
        orderList = new OrderListPanel(menu, actions);
        RoundedPanel entry = new RoundedPanel(new BorderLayout(0, 10));
        UiSupport.padding(entry, 18);
        entry.add(UiSupport.label("Order details", 16, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
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

        RoundedPanel preview = new RoundedPanel(new BorderLayout(0, 10), new Color(235, 241, 232));
        UiSupport.padding(preview, 18);
        preview.add(UiSupport.label("Order summary", 16, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        JPanel breakdown = UiSupport.transparent(new GridLayout(2, 1, 0, 8));
        breakdown.add(UiSupport.pair(UiSupport.label("Subtotal", 13, Font.PLAIN, Theme.MUTED), subtotal));
        breakdown.add(UiSupport.pair(UiSupport.label("Discount", 13, Font.PLAIN, Theme.MUTED), savings));
        JPanel summary = UiSupport.transparent(new BorderLayout(0, 10));
        summary.add(breakdown, BorderLayout.NORTH);
        JPanel totalBlock = UiSupport.transparent(new BorderLayout(0, 6));
        totalBlock.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(207, 219, 203)),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        totalBlock.add(UiSupport.label("TOTAL TO PAY", 11, Font.BOLD, Theme.MUTED), BorderLayout.NORTH);
        totalBlock.add(total, BorderLayout.CENTER);
        summary.add(totalBlock, BorderLayout.CENTER);
        preview.add(summary, BorderLayout.CENTER);
        feedback.setPreferredSize(new Dimension(270, 32));
        preview.add(feedback, BorderLayout.SOUTH);
        JPanel columns = UiSupport.transparent(new GridLayout(1, 2, 18, 0));
        columns.add(entry);
        columns.add(preview);
        columns.setPreferredSize(new Dimension(0, 280));
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
            feedback.setText(storageReady ? "Ready to add." : "Order history is unavailable. Use Refresh to reload.");
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
        return new Dimension(super.getPreferredSize().width, 620);
    }
}

