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
    private final JLabel orderCount = UiSupport.label("0", 24, Font.BOLD, Theme.TEXT);
    private final JLabel portions = UiSupport.label("0", 24, Font.BOLD, Theme.TEXT);
    private final JLabel revenue = UiSupport.label("PHP 0.00", 24, Font.BOLD, Theme.PRIMARY);
    private final JLabel status = UiSupport.label("Loading order history…", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel sidebarCount = UiSupport.label("0 current orders", 12, Font.PLAIN, Theme.ON_DARK_MUTED);
    private final ReceiptPanel receiptHistory;

    public AppShell(OrderActions actions, MenuCatalog menu, AppConfig config) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(buildSidebar(actions), BorderLayout.WEST);
        JPanel canvas = new JPanel(new BorderLayout(0, Theme.GAP));
        canvas.setBackground(Theme.BACKGROUND);
        canvas.setBorder(BorderFactory.createEmptyBorder(18, 24, 14, 24));
        JPanel top = UiSupport.transparent(new BorderLayout(0, 12));
        JLabel date = UiSupport.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)),
                12, Font.PLAIN, Theme.MUTED);
        date.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(UiSupport.pair(UiSupport.label("THE COUNTER  /  STEIN FOOD HUB", 11, Font.BOLD, Theme.PRIMARY), date), BorderLayout.NORTH);
        JPanel statistics = UiSupport.transparent(new GridLayout(1, 3, 12, 0));
        statistics.add(metric("Current orders", orderCount, AppIcon.Kind.ORDERS));
        statistics.add(metric("Total portions", portions, AppIcon.Kind.TRAY));
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
        sidebar.setPreferredSize(new Dimension(208, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 22, 16));
        JPanel brand = UiSupport.transparent(new BorderLayout(0, 14));
        RoundedPanel mark = new RoundedPanel(new BorderLayout(), Theme.ON_DARK);
        mark.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
        mark.add(new JLabel(new AppIcon(AppIcon.Kind.BURGER, 30, Theme.SIDEBAR)));
        JPanel symbol = UiSupport.transparent(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        symbol.add(mark);
        brand.add(symbol, BorderLayout.NORTH);
        JPanel name = UiSupport.transparent(new BorderLayout(0, 2));
        name.add(UiSupport.label("Stein", 34, Font.BOLD, Theme.ON_DARK), BorderLayout.NORTH);
        name.add(UiSupport.label("F O O D   H U B", 12, Font.BOLD, Theme.ACCENT), BorderLayout.CENTER);
        brand.add(name, BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 12, 30, 0));
        JPanel sidebarTop = UiSupport.transparent(new BorderLayout());
        sidebarTop.add(brand, BorderLayout.NORTH);
        JPanel links = UiSupport.transparent(null);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        JLabel section = UiSupport.label("COUNTER SERVICE", 10, Font.BOLD, Theme.ON_DARK_MUTED);
        section.setBorder(BorderFactory.createEmptyBorder(0, 14, 12, 0));
        section.setAlignmentX(0);
        links.add(section);
        addNav(links, actions, "new", "New order", AppIcon.Kind.PLUS);
        addNav(links, actions, "receipt", "Receipts", AppIcon.Kind.RECEIPT);
        links.add(Box.createVerticalStrut(16));
        addNav(links, actions, "settings", "Settings", AppIcon.Kind.SETTINGS);
        sidebarTop.add(links, BorderLayout.CENTER);
        sidebar.add(sidebarTop, BorderLayout.NORTH);
        JPanel footer = UiSupport.transparent(new BorderLayout(0, 16));
        JPanel detail = UiSupport.transparent(new BorderLayout(0, 7));
        detail.add(UiSupport.label("GOOD FOOD. EVERY DAY.", 10, Font.BOLD, Theme.ACCENT), BorderLayout.NORTH);
        detail.add(sidebarCount, BorderLayout.CENTER);
        detail.add(UiSupport.label("Your everyday order counter", 11, Font.PLAIN, Theme.ON_DARK_MUTED), BorderLayout.SOUTH);
        footer.add(detail, BorderLayout.CENTER);
        JPanel stripe = new JPanel() {
            @Override protected void paintComponent(java.awt.Graphics graphics) {
                graphics.setColor(Theme.ACCENT);
                int tile = 8;
                for (int x = 0; x < getWidth(); x += tile) {
                    graphics.fillRect(x, (x / tile % 2) * tile, tile, tile);
                }
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(0, 16));
        footer.add(stripe, BorderLayout.SOUTH);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addNav(JPanel links, OrderActions actions, String id, String text, AppIcon.Kind icon) {
        AppButton button = new AppButton(text, AppButton.Style.NAVIGATION);
        button.setName("nav-" + id);
        button.setIcon(new AppIcon(icon, 18, Theme.ON_DARK));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(176, 48));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setAlignmentX(0);
        button.addActionListener(event -> actions.navigate(id));
        navigation.put(id, button);
        links.add(button);
        links.add(Box.createVerticalStrut(6));
    }

    private JPanel metric(String title, JLabel value, AppIcon.Kind kind) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.setPreferredSize(new Dimension(0, 76));
        RoundedPanel icon = new RoundedPanel(new BorderLayout(), Theme.PRIMARY_SOFT);
        icon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        icon.add(new JLabel(new AppIcon(kind, 20, Theme.PRIMARY)));
        JPanel iconSlot = UiSupport.transparent(new java.awt.GridBagLayout());
        iconSlot.add(icon);
        card.add(iconSlot, BorderLayout.WEST);
        JPanel detail = UiSupport.transparent(new BorderLayout(0, 1));
        detail.add(UiSupport.label(title, 12, Font.PLAIN, Theme.MUTED), BorderLayout.NORTH);
        detail.add(value, BorderLayout.CENTER);
        card.add(detail, BorderLayout.CENTER);
        return card;
    }

    private void addWorkspace(String id, WorkspacePanel panel) {
        workspaces.put(id, panel);
        JScrollPane scroll = new JScrollPane(panel);
        Theme.styleScrollPane(scroll);
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
