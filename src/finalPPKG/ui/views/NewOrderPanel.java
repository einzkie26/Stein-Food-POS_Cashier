package finalPPKG.ui.views;

import finalPPKG.app.OrderActions;
import finalPPKG.model.MenuItem;
import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.MenuIcons;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Entry, live totals and the complete current order list share one screen. */
public final class NewOrderPanel extends WorkspacePanel {
    private final OrderFormPanel form;
    private final OrderListPanel orderList;
    private final RoundedPanel entry = new RoundedPanel(new BorderLayout(0, 12));
    private final AppButton save = new AppButton("Add to order", AppButton.Style.PRIMARY);
    private final JLabel subtotal = UiSupport.label("PHP 0.00", 12, Font.BOLD, Theme.TEXT);
    private final JLabel savings = UiSupport.label("PHP 0.00", 12, Font.BOLD, Theme.SUCCESS);
    private final JLabel total = UiSupport.label("PHP 0.00", 28, Font.BOLD, Theme.PRIMARY);
    private final JLabel feedback = UiSupport.label("", 12, Font.PLAIN, Theme.MUTED);
    private boolean storageReady;

    public NewOrderPanel(List<MenuItem> menu, OrderActions actions) {
        super("New order");
        form = new OrderFormPanel(menu, null, buildSummary());
        orderList = new OrderListPanel(menu, actions);
        UiSupport.padding(entry, 16);
        JPanel heading = UiSupport.transparent(new BorderLayout(12, 0));
        JPanel title = UiSupport.transparent(new BorderLayout(0, 3));
        title.add(UiSupport.label("Order details", 17, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        feedback.setPreferredSize(new Dimension(0, 18));
        title.add(feedback, BorderLayout.CENTER);
        heading.add(title, BorderLayout.CENTER);
        JPanel entryActions = UiSupport.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        entry.add(form, BorderLayout.CENTER);
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
        entryActions.add(reset);
        entryActions.add(save);
        heading.add(entryActions, BorderLayout.EAST);
        entry.add(heading, BorderLayout.NORTH);
        body.add(entry, BorderLayout.NORTH);
        body.add(orderList, BorderLayout.CENTER);
        form.setChangeListener(this::refreshPreview);
        refreshPreview();
    }

    private JPanel buildSummary() {
        RoundedPanel summary = new RoundedPanel(new BorderLayout(10, 0), Theme.SUMMARY);
        summary.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JPanel breakdown = UiSupport.transparent(new GridLayout(2, 1, 0, 4));
        breakdown.add(UiSupport.pair(UiSupport.label("Subtotal", 11, Font.PLAIN, Theme.MUTED), subtotal));
        breakdown.add(UiSupport.pair(UiSupport.label("Discount", 11, Font.PLAIN, Theme.MUTED), savings));
        summary.add(breakdown, BorderLayout.WEST);
        JPanel totalBlock = UiSupport.transparent(new BorderLayout(0, 2));
        JLabel caption = UiSupport.label("ITEM TOTAL", 10, Font.BOLD, Theme.MUTED);
        caption.setIcon(MenuIcons.orderTotal(18));
        caption.setIconTextGap(6);
        totalBlock.add(caption, BorderLayout.NORTH);
        totalBlock.add(total, BorderLayout.CENTER);
        summary.add(totalBlock, BorderLayout.CENTER);
        return summary;
    }

    private void refreshPreview() {
        try {
            Order order = form.readOrder();
            subtotal.setText(UiSupport.money(order.getSubtotal()));
            savings.setText("− " + UiSupport.money(order.getDiscountAmount()));
            total.setText(UiSupport.money(order.getTotal()));
            feedback.setText(storageReady ? "Choose a card, then add it to your order." : "Order history unavailable. Use Refresh to reload.");
            feedback.setForeground(storageReady ? Theme.SUCCESS : Theme.MUTED);
            feedback.setToolTipText(null);
            save.setEnabled(storageReady);
        } catch (IllegalArgumentException exception) {
            subtotal.setText(UiSupport.money(BigDecimal.ZERO));
            savings.setText(UiSupport.money(BigDecimal.ZERO));
            total.setText(UiSupport.money(BigDecimal.ZERO));
            feedback.setText(exception.getMessage());
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
        return new Dimension(super.getPreferredSize().width, Math.max(640,
                entry.getPreferredSize().height + 240 + Theme.GAP + 52));
    }
}

