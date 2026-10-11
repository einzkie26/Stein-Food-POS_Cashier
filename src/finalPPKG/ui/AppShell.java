package finalPPKG.ui;

import finalPPKG.app.AppConfig;
import finalPPKG.app.OrderActions;
import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import finalPPKG.service.MenuCatalog;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import finalPPKG.ui.views.NewOrderPanel;
import finalPPKG.ui.views.ReceiptPanel;
import finalPPKG.ui.views.SettingsPanel;
import finalPPKG.ui.views.WorkspacePanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/** Navigation and shared statistics surround independent workspace screens. */
public final class AppShell extends JPanel {
    private final CardLayout cards = new CardLayout();
    private final JPanel pages = UiSupport.transparent(cards);
    private final Map<String, WorkspacePanel> workspaces = new LinkedHashMap<>();
    private final Map<String, AppButton> navigation = new LinkedHashMap<>();
    private final JLabel orderCount = UiSupport.label("0", 26, Font.BOLD, Theme.TEXT);
    private final JLabel portions = UiSupport.label("0", 26, Font.BOLD, Theme.TEXT);
    private final JLabel revenue = UiSupport.label("PHP 0.00", 26, Font.BOLD, Theme.TEXT);
    private final JLabel status = UiSupport.label("Loading order history…", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel sidebarCount = UiSupport.label("0 saved orders", 12, Font.PLAIN, new Color(0xC0CDC5));
    private final ReceiptPanel receiptHistory;

    public AppShell(OrderActions actions, MenuCatalog menu, AppConfig config) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(buildSidebar(actions), BorderLayout.WEST);
        JPanel canvas = new JPanel(new BorderLayout(0, 20));
        canvas.setBackground(Theme.BACKGROUND);
        canvas.setBorder(BorderFactory.createEmptyBorder(24, 26, 16, 26));
        JPanel top = UiSupport.transparent(new BorderLayout(0, 20));
        JLabel date = UiSupport.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)),
                12, Font.PLAIN, Theme.MUTED);
        date.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(UiSupport.pair(UiSupport.label("YOUR ORDER DESK", 11, Font.BOLD, Theme.MUTED), date), BorderLayout.NORTH);
        JPanel statistics = UiSupport.transparent(new GridLayout(1, 3, 14, 0));
        statistics.add(metric("Current orders", orderCount, AppIcon.Kind.ORDERS));
        statistics.add(metric("Total portions", portions, AppIcon.Kind.LEAF));
        statistics.add(metric("Order total", revenue, AppIcon.Kind.RECEIPT));
        top.add(statistics, BorderLayout.CENTER);
        canvas.add(top, BorderLayout.NORTH);
        addWorkspace("new", new NewOrderPanel(menu.getItems(), actions));
        receiptHistory = new ReceiptPanel(actions);
        addWorkspace("receipt", receiptHistory);
        addWorkspace("settings", new SettingsPanel(config, actions));
        canvas.add(pages, BorderLayout.CENTER);
        canvas.add(status, BorderLayout.SOUTH);
        add(canvas, BorderLayout.CENTER);
        updateOrders(List.of());
        updateReceipts(List.of());
        setStorageReady(false);
        setReceiptHistoryReady(false);
        showPage("new");
    }

    private JPanel buildSidebar(OrderActions actions) {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(218, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 18, 24, 18));
        JPanel brand = UiSupport.transparent(new BorderLayout(0, 10));
        JLabel symbol = new JLabel(new AppIcon(AppIcon.Kind.LEAF, 32, new Color(0xD6DFCA)));
        symbol.setHorizontalAlignment(SwingConstants.LEFT);
        brand.add(symbol, BorderLayout.NORTH);
        JPanel name = UiSupport.transparent(new BorderLayout(0, 5));
        name.add(UiSupport.label("Stein Food Hub", 20, Font.BOLD, Theme.SURFACE), BorderLayout.NORTH);
        brand.add(name, BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 34, 0));
        JPanel sidebarTop = UiSupport.transparent(new BorderLayout());
        sidebarTop.add(brand, BorderLayout.NORTH);
        JPanel links = UiSupport.transparent(null);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        JLabel section = UiSupport.label("WORKSPACE", 10, Font.BOLD, new Color(0x839589));
        section.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 0));
        section.setAlignmentX(0);
        links.add(section);
        addNav(links, actions, "new", "New order", AppIcon.Kind.PLUS);
        addNav(links, actions, "receipt", "Receipts", AppIcon.Kind.RECEIPT);
        links.add(Box.createVerticalStrut(22));
        addNav(links, actions, "settings", "Settings", AppIcon.Kind.SETTINGS);
        sidebarTop.add(links, BorderLayout.CENTER);
        sidebar.add(sidebarTop, BorderLayout.NORTH);
        JPanel footer = UiSupport.transparent(new BorderLayout(0, 8));
        JLabel badge = UiSupport.label("●  LOCAL WORKSPACE", 10, Font.BOLD, new Color(0xA9C99D));
        footer.add(badge, BorderLayout.NORTH);
        footer.add(sidebarCount, BorderLayout.CENTER);
        footer.add(UiSupport.label("Made for everyday orders", 11, Font.PLAIN, new Color(0x839589)), BorderLayout.SOUTH);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNav(JPanel links, OrderActions actions, String id, String text, AppIcon.Kind icon) {
        AppButton button = new AppButton(text, AppButton.Style.NAVIGATION);
        button.setName("nav-" + id);
        button.setIcon(new AppIcon(icon, 18, new Color(0xC0CDC5)));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(0);
        button.addActionListener(event -> actions.navigate(id));
        navigation.put(id, button);
        links.add(button);
        links.add(Box.createVerticalStrut(6));
    }

    private JPanel metric(String title, JLabel value, AppIcon.Kind kind) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(10, 6));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.add(UiSupport.label(title, 12, Font.PLAIN, Theme.MUTED), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(new JLabel(new AppIcon(kind, 22, Theme.SUCCESS)), BorderLayout.EAST);
        return card;
    }

    private void addWorkspace(String id, WorkspacePanel panel) {
        workspaces.put(id, panel);
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        pages.add(scroll, id);
    }

    public void showPage(String id) {
        if (!workspaces.containsKey(id)) { throw new IllegalArgumentException("Unknown workspace: " + id); }
        cards.show(pages, id);
        navigation.forEach((page, button) -> button.setActive(page.equals(id)));
    }

    public void updateOrders(List<Order> orders) {
        orderCount.setText(Integer.toString(orders.size()));
        portions.setText(Long.toString(orders.stream().mapToLong(Order::getQuantity).sum()));
        BigDecimal total = orders.stream().map(Order::getTotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
        revenue.setText(UiSupport.money(total));
        sidebarCount.setText(orders.size() + " current " + (orders.size() == 1 ? "order" : "orders"));
        workspaces.values().forEach(panel -> panel.onOrdersChanged(orders));
    }

    public void setCompactRows(boolean compact) { workspaces.values().forEach(panel -> panel.setCompactRows(compact)); }
    public void updateReceipts(List<Receipt> receipts) { receiptHistory.onReceiptsChanged(receipts); }
    public void setReceiptHistoryReady(boolean ready) { receiptHistory.setHistoryReady(ready); }
    public void setStorageReady(boolean ready) { workspaces.values().forEach(panel -> panel.setStorageReady(ready)); }

    public void setStatus(String message, boolean error) {
        status.setText(message);
        status.setToolTipText(message);
        status.setForeground(error ? Theme.DANGER : Theme.MUTED);
    }
}
