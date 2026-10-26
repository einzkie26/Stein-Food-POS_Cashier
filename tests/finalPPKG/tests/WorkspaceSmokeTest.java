package finalPPKG.tests;

import finalPPKG.app.AppConfig;
import finalPPKG.app.OrderActions;
import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import finalPPKG.service.MenuCatalog;
import finalPPKG.ui.AppShell;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.MenuCard;
import finalPPKG.ui.components.MenuCardPicker;
import finalPPKG.ui.components.MenuIcons;
import finalPPKG.ui.components.SidebarLogo;
import finalPPKG.ui.theme.Theme;
import finalPPKG.ui.views.OrderFormPanel;
import finalPPKG.ui.views.ReceiptDialogPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;

/** Exercises real Swing panels on the EDT without opening a window or writing order history. */
public final class WorkspaceSmokeTest {
    private WorkspaceSmokeTest() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                runChecks();
            } catch (Throwable error) {
                failure.set(error);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError("Workspace smoke test failed", failure.get());
        }
        System.out.println("PASS: Swing validation, embedded orders, checkout, receipt cleanup, and modal actions.");
        System.out.println("Screen previews: build/preview/");
    }

    private static void runChecks() throws Exception {
        Theme.install();
        MenuCatalog menu = new MenuCatalog();
        FakeActions actions = new FakeActions();
        AppShell shell = new AppShell(actions, menu, new AppConfig(Path.of("build", "smoke-data")));
        actions.shell = shell;
        checkWorkspace(shell, menu, actions);
    }

    private static void checkWorkspace(AppShell shell, MenuCatalog menu, FakeActions actions) throws Exception {
        shell.setStorageReady(true);
        shell.setSize(1160, 740);
        shell.showPage("new");
        layout(shell);
        SidebarLogo logo = named(shell, "sidebar-logo", SidebarLogo.class);
        truth(logo.getText() == null || logo.getText().isBlank(), "sidebar wordmark uses the supplied images");
        truth(find(shell, JLabel.class, label -> "Stein".equals(label.getText())) == null,
                "old sidebar Stein text is removed");
        truth(!(named(shell, "main-logo", JLabel.class).getIcon() instanceof AppIcon),
                "provided main logo loads from the classpath");
        for (int frame = 1; frame <= 5; frame++) {
            truth(!(MenuIcons.sidebarFrame(frame, 176) instanceof AppIcon),
                    "provided sidebar animation frame loads: " + frame);
        }
        truth(find(shell, AbstractButton.class, component -> "nav-orders".equals(component.getName())) == null,
                "orders have no separate navigation page");
        truth(!named(shell, "checkout", AbstractButton.class).isEnabled(), "an empty order list cannot checkout");
        checkMenuCards(shell, menu);

        JTextField price = named(shell, "unitPrice", JTextField.class);
        JTextField discount = named(shell, "discount", JTextField.class);
        JSpinner quantity = named(shell, "quantity", JSpinner.class);
        AbstractButton save = named(shell, "saveOrder", AbstractButton.class);
        truth(!save.isEnabled(), "blank price cannot be saved");
        quantity.setValue(3);
        price.setText("19.99");
        discount.setText("12.5");
        truth(save.isEnabled(), "valid details enable saving");
        shell.setStorageReady(false);
        truth(!save.isEnabled(), "load failure disables saving even for valid input");
        save.doClick(0);
        equal(0, actions.added, "locked history cannot be changed");
        shell.setStorageReady(true);
        truth(save.isEnabled(), "successful reload unlocks valid input");
        save.doClick(0);
        equal(1, actions.added, "save requests one mutation");
        equal(3, actions.lastOrder.getQuantity(), "typed quantity is read");
        money("52.47", actions.lastOrder.getTotal(), "form total matches domain total");
        truth(price.getText().isBlank() && !save.isEnabled(), "successful save resets the form");

        price.setText("10.00");
        discount.setText("");
        truth(save.isEnabled(), "blank discount means zero discount");
        for (String invalidPrice : List.of("NaN", "Infinity", "abc", "-0.01", "1e9")) {
            price.setText(invalidPrice);
            truth(!save.isEnabled(), "invalid price is rejected: " + invalidPrice);
            save.doClick(0);
        }
        equal(1, actions.added, "invalid form never invokes add");
        price.setText("10");
        for (String invalidDiscount : List.of("NaN", "-1", "101")) {
            discount.setText(invalidDiscount);
            truth(!save.isEnabled(), "invalid discount is rejected: " + invalidDiscount);
        }
        discount.setText("0");
        JTextField quantityEditor = ((JSpinner.DefaultEditor) quantity.getEditor()).getTextField();
        for (String invalidQuantity : List.of("0", "-1", "1.5", "many", "10000")) {
            quantityEditor.setText(invalidQuantity);
            truth(!save.isEnabled(), "invalid typed quantity is rejected: " + invalidQuantity);
        }
        quantityEditor.setText("2");
        truth(save.isEnabled(), "valid typed quantity restores save");
        quantityEditor.setText("9999");
        equal(1, quantity.getValue(), "typed editor text does not commit the spinner model yet");
        button(shell, "Reset").doClick(0);
        equal("1", quantityEditor.getText(), "reset replaces uncommitted quantity text");
        price.setText("10");
        equal(1, visible(shell, OrderFormPanel.class).readOrder().getQuantity(), "reset quantity is read as one");
        quantityEditor.setText("2");

        Order savedDish = new Order("A legacy dish", 2, new BigDecimal("7.50"), BigDecimal.ZERO);
        OrderFormPanel editForm = new OrderFormPanel(menu.getItems(), savedDish);
        Order prefilled = editForm.readOrder();
        equal(savedDish, prefilled, "editing preserves saved dishes absent from the menu");

        List<Order> orders = List.of(order("Rice", 1, "5"), order("Fish Fillet", 2, "12"),
                order("Rice", 3, "7"));
        shell.updateOrders(orders);
        actions.currentOrders = orders;
        shell.showPage("new");
        layout(shell);
        JTable table = visible(shell, JTable.class);
        JTextField search = named(shell, "searchOrders", JTextField.class);
        AbstractButton remove = button(shell, "Remove selected");
        truth(!remove.isEnabled(), "no selection disables removal");
        search.setText("rice");
        equal(2, table.getRowCount(), "dish search is case-insensitive");
        table.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.DESCENDING)));
        table.setRowSelectionInterval(0, 0);
        equal(2, table.convertRowIndexToModel(0), "filtered descending row points to original index");
        shell.setStorageReady(false);
        truth(!remove.isEnabled(), "load failure disables removing a selected row");
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 0, 0,
                12, table.getRowHeight() / 2, 2, false, MouseEvent.BUTTON1));
        truth(!button(shell, "Edit").isEnabled(), "double-click cannot edit locked history");
        shell.setStorageReady(true);
        int blankRowY = table.getRowCount() * table.getRowHeight() + 10;
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 0, 0,
                12, blankRowY, 2, false, MouseEvent.BUTTON1));
        remove.doClick(0);
        equal(2, actions.removedIndex, "remove action uses the model index");
        search.setText("[rice]");
        equal(0, table.getRowCount(), "search handles regex characters as literal text");
        search.setText("");
        shell.updateOrders(orders);
        truth(!remove.isEnabled(), "data refresh clears stale selection");
        named(shell, "checkout", AbstractButton.class).doClick(0);
        equal(1, actions.checkouts, "checkout is available on the new-order screen");
        equal(3, actions.lastReceipt.getOrders().size(), "checkout includes the complete order list");
        money("50.00", actions.lastReceipt.getTotal(), "checkout total includes every current order");
        int regularHeight = table.getRowHeight();
        shell.setCompactRows(true);
        truth(table.getRowHeight() < regularHeight, "compact mode decreases table row height");
        shell.setCompactRows(false);
        equal(regularHeight, table.getRowHeight(), "normal row height is restored");
        shell.setStatus("Smoke check complete", false);

        Receipt older = new Receipt(UUID.fromString("11111111-0000-0000-0000-000000000001"),
                Instant.parse("2026-10-10T01:00:00Z"), List.of(order("Rice", 1, "5")), "");
        Receipt newer = new Receipt(UUID.fromString("22222222-0000-0000-0000-000000000002"),
                Instant.parse("2026-10-11T02:00:00Z"), List.of(order("Fish Fillet", 2, "12")), "exported-receipt.txt");
        shell.updateReceipts(List.of(older, newer));
        shell.showPage("receipt");
        layout(shell);
        JTable history = named(shell, "receiptHistory", JTable.class);
        equal(2, history.getRowCount(), "saved and exported receipts appear in history");
        AbstractButton deleteReceipts = named(shell, "deleteReceipts", AbstractButton.class);
        AbstractButton clearCache = named(shell, "clearReceiptCache", AbstractButton.class);
        truth(!deleteReceipts.isEnabled() && !clearCache.isEnabled(), "receipt cleanup waits for a successful history load");
        shell.setReceiptHistoryReady(true);
        truth(clearCache.isEnabled() && !deleteReceipts.isEnabled(), "nonempty history can be cleared, with deletion requiring checked rows");
        history.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.DESCENDING)));
        shell.setStorageReady(false);
        Rectangle receiptCell = history.getCellRect(0, 1, true);
        history.dispatchEvent(new MouseEvent(history, MouseEvent.MOUSE_CLICKED, 0, 0,
                receiptCell.x + 12, receiptCell.y + receiptCell.height / 2, 1, false, MouseEvent.BUTTON1));
        equal(newer.getId(), actions.lastOpened.getId(), "clicking a sorted history row opens the correct receipt");
        shell.setStorageReady(true);
        history.setRowSelectionInterval(1, 1);
        named(shell, "openReceipt", AbstractButton.class).doClick(0);
        equal(older.getId(), actions.lastOpened.getId(), "open button uses the selected history model row");
        int opens = actions.receiptOpens;
        toggleReceiptCheckbox(history, 0);
        equal(opens, actions.receiptOpens, "clicking a selection checkbox does not open a modal");
        truth(deleteReceipts.isEnabled(), "checking a receipt enables deletion");
        history.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(1, SortOrder.ASCENDING)));
        truth(Boolean.TRUE.equals(history.getValueAt(1, 0)), "checkbox selection follows its receipt after sorting");
        deleteReceipts.doClick(0);
        equal(List.of(newer.getId()), actions.deletedReceipts, "deletion uses receipt IDs after sorting");
        toggleReceiptCheckbox(history, 0);
        deleteReceipts.doClick(0);
        equal(List.of(older.getId(), newer.getId()), actions.deletedReceipts, "multiple checked receipts can be deleted together");
        toggleReceiptCheckbox(history, 0);
        truth(!Boolean.TRUE.equals(history.getValueAt(0, 0)), "clicking a checked box removes it from deletion selection");
        shell.setReceiptHistoryReady(false);
        truth(!deleteReceipts.isEnabled() && !clearCache.isEnabled() && !history.isCellEditable(0, 0),
                "failed receipt loading locks cleanup and checkbox editing");
        deleteReceipts.doClick(0);
        clearCache.doClick(0);
        equal(2, actions.receiptDeletes, "locked cleanup cannot request deletion");
        equal(0, actions.receiptClears, "locked cleanup cannot request clearing");
        shell.setReceiptHistoryReady(true);
        truth(!deleteReceipts.isEnabled(), "reload cannot reuse stale checked receipts");
        toggleReceiptCheckbox(history, 0);
        shell.updateReceipts(List.of(newer));
        truth(!deleteReceipts.isEnabled(), "receipt updates clear obsolete deletion selections");
        clearCache.doClick(0);
        equal(1, actions.receiptClears, "clear-cache action is wired independently of selection");
        shell.updateReceipts(List.of());
        truth(!deleteReceipts.isEnabled() && !clearCache.isEnabled()
                        && !named(shell, "openReceipt", AbstractButton.class).isEnabled(),
                "empty history disables all receipt actions");
        shell.updateReceipts(List.of(older, newer));
        history.getRowSorter().setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.DESCENDING)));
        toggleReceiptCheckbox(history, 0);

        shell.showPage("new");
        layout(shell);
        JScrollPane workspaceScroll = visible(shell, JScrollPane.class);
        truth(workspaceScroll.getViewport().getView().getHeight() >= workspaceScroll.getViewport().getExtentSize().height,
                "small window retains a usable workspace height");
        save.scrollRectToVisible(new Rectangle(0, 0, save.getWidth(), save.getHeight()));
        Rectangle saveBounds = SwingUtilities.convertRectangle(save.getParent(), save.getBounds(), shell);
        truth(new Rectangle(0, 0, shell.getWidth(), shell.getHeight()).contains(saveBounds),
                "save button is reachable in a smaller window");
        workspaceScroll.getVerticalScrollBar().setValue(0);
        shell.setSize(1240, 842);

        Path previewDirectory = Path.of("build", "preview").toAbsolutePath().normalize();
        Files.createDirectories(previewDirectory);
        String[][] pages = {{"new", "New order"},
                {"receipt", "Receipt history"}, {"settings", "Settings"}};
        for (String[] page : pages) {
            named(shell, "nav-" + page[0], AbstractButton.class).doClick(0);
            layout(shell);
            truth(hasVisibleLabel(shell, page[1]), "page title is visible: " + page[1]);
            truth(find(shell, JLabel.class, label -> label.getText() != null
                    && (label.getText().startsWith("Find an order,")
                    || label.getText().startsWith("Good food, made simple.")
                    || label.getText().startsWith("A clear breakdown")
                    || label.getText().startsWith("Keep your workspace"))) == null, "page subtitles are removed");
            assertNamedControlBounds(shell, shell);
            assertDefaultViewportContent(shell, page[0]);
            BufferedImage preview = new BufferedImage(shell.getWidth(), shell.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = preview.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                shell.printAll(graphics);
            } finally {
                graphics.dispose();
            }
            ImageIO.write(preview, "png", previewDirectory.resolve(page[0] + ".png").toFile());
        }
        checkSmallMenuLayout(shell);
        checkReceiptModal(newer, actions, previewDirectory);
    }

    private static void checkMenuCards(AppShell shell, MenuCatalog menu) {
        MenuCardPicker picker = visible(shell, MenuCardPicker.class);
        equal(menu.getItems().size(), picker.getComponentCount(), "every menu dish has a card");
        for (Component component : picker.getComponents()) {
            truth(component instanceof MenuCard card && !(card.getIcon() instanceof AppIcon),
                    "every provided dish icon is loaded from classpath resources");
        }
        truth(!(MenuIcons.currentOrder(32) instanceof AppIcon), "provided current-order icon loads");
        truth(!(MenuIcons.orderTotal(32) instanceof AppIcon), "provided order-total icon loads");
        JTextField price = named(shell, "unitPrice", JTextField.class);
        MenuCard steak = (MenuCard) button(shell, "Pork Steak");
        MenuCard fish = (MenuCard) button(shell, "Fish Fillet");
        steak.doClick(0);
        price.setText("85.50");
        steak.doClick(0);
        equal("85.50", price.getText(), "clicking the selected card preserves the entered price");
        fish.doClick(0);
        truth(fish.isSelected() && !steak.isSelected(), "menu cards keep exactly one selected dish");
        truth(price.getText().isBlank(), "a different dish clears its predecessor's price");
        price.setText("25.75");
        equal("Fish Fillet", visible(shell, OrderFormPanel.class).readOrder().getDish(),
                "card selection is used by the validated order form");
        picker.getActionMap().get("next").actionPerformed(new java.awt.event.ActionEvent(picker, 0, "next"));
        truth(price.getText().isBlank(), "keyboard dish selection also clears the previous price");
        price.setText("20");
        equal("Sinabawang Isda Barilison", visible(shell, OrderFormPanel.class).readOrder().getDish(),
                "arrow navigation selects the next menu dish");
        button(shell, menu.getItems().get(0).getName()).doClick(0);
        button(shell, "Reset").doClick(0);
    }

    private static void checkSmallMenuLayout(AppShell shell) {
        shell.showPage("new");
        shell.setSize(980, 622);
        layout(shell);
        MenuCardPicker picker = visible(shell, MenuCardPicker.class);
        JScrollPane workspace = visible(shell, JScrollPane.class);
        for (Component component : picker.getComponents()) {
            truth(component.getWidth() >= 130 && component.getHeight() >= 60,
                    "menu cards remain usable at minimum window width");
            truth(new Rectangle(0, 0, picker.getWidth(), picker.getHeight()).contains(component.getBounds()),
                    "responsive card grid does not overflow");
            ((javax.swing.JComponent) component).scrollRectToVisible(
                    new Rectangle(0, 0, component.getWidth(), component.getHeight()));
            assertInViewport(component, workspace);
        }
        for (String name : List.of("unitPrice", "saveOrder", "checkout")) {
            javax.swing.JComponent control = named(shell, name, javax.swing.JComponent.class);
            control.scrollRectToVisible(new Rectangle(0, 0, control.getWidth(), control.getHeight()));
            assertInViewport(control, workspace);
        }
    }

    private static void checkReceiptModal(Receipt receipt, FakeActions actions, Path previewDirectory) throws Exception {
        int[] closed = {0};
        ReceiptDialogPanel panel = new ReceiptDialogPanel(receipt, actions, false, true, () -> closed[0]++);
        panel.setSize(700, 660);
        layout(panel);
        String text = named(panel, "receiptPreview", JTextArea.class).getText();
        truth(text.contains(receipt.getNumber()) && text.contains("Fish Fillet") && text.contains("24.00"),
                "modal preview uses the receipt snapshot");
        named(panel, "saveReceipt", AbstractButton.class).doClick(0);
        equal(1, actions.receiptSaves, "modal saves the receipt to history");
        truth(!named(panel, "saveReceipt", AbstractButton.class).isEnabled(), "saved receipt cannot be duplicated");
        named(panel, "exportReceipt", AbstractButton.class).doClick(0);
        equal(1, actions.receiptExports, "modal exports the receipt snapshot");
        named(panel, "printReceipt", AbstractButton.class).doClick(0);
        equal(receipt.getId(), actions.lastPrinted.getId(), "modal prints the chosen receipt snapshot");
        button(panel, "Close").doClick(0);
        equal(1, closed[0], "modal close action is wired");
        BufferedImage image = new BufferedImage(700, 660, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            panel.printAll(graphics);
        } finally { graphics.dispose(); }
        ImageIO.write(image, "png", previewDirectory.resolve("checkout.png").toFile());
        ReceiptDialogPanel locked = new ReceiptDialogPanel(receipt, actions, true, false, () -> { });
        truth(!named(locked, "saveReceipt", AbstractButton.class).isEnabled(), "unavailable history disables receipt saving");
        truth(!named(locked, "exportReceipt", AbstractButton.class).isEnabled(), "unavailable history disables exporting");
        truth(named(locked, "printReceipt", AbstractButton.class).isEnabled(), "existing snapshots remain printable");
    }

    private static Order order(String name, int quantity, String price) {
        return new Order(name, quantity, new BigDecimal(price), BigDecimal.ZERO);
    }

    private static void toggleReceiptCheckbox(JTable table, int row) {
        truth(table.editCellAt(row, 0), "receipt selection cell can be edited");
        Component editor = table.getEditorComponent();
        truth(editor instanceof AbstractButton, "receipt selection uses a checkbox editor");
        ((AbstractButton) editor).doClick(0);
        Rectangle cell = table.getCellRect(row, 0, true);
        table.dispatchEvent(new MouseEvent(table, MouseEvent.MOUSE_CLICKED, 0, 0,
                cell.x + cell.width / 2, cell.y + cell.height / 2, 1, false, MouseEvent.BUTTON1));
    }

    private static void layout(Container container) {
        // A responsive grid needs its actual width before the viewport can settle on its height.
        for (int pass = 0; pass < 3; pass++) { layoutTree(container); }
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) { layoutTree(nested); }
        }
    }

    private static void assertNamedControlBounds(Container container, AppShell shell) {
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) { continue; }
            if (child.getName() != null && List.of("unitPrice", "discount", "quantity", "dish",
                    "saveOrder", "searchOrders").contains(child.getName())) {
                Rectangle bounds = SwingUtilities.convertRectangle(child.getParent(), child.getBounds(), shell);
                truth(bounds.width > 30 && bounds.height > 20, "usable control size: " + child.getName());
                truth(new Rectangle(0, 0, shell.getWidth(), shell.getHeight()).contains(bounds),
                        "control remains inside workspace: " + child.getName());
            }
            if (child instanceof Container nested) { assertNamedControlBounds(nested, shell); }
        }
    }

    private static void assertDefaultViewportContent(AppShell shell, String page) {
        JScrollPane workspace = visible(shell, JScrollPane.class);
        if (page.equals("new")) {
            JLabel total = find(shell, JLabel.class,
                    label -> "PHP 20.00".equals(label.getText()) && label.getFont().getSize() >= 28);
            truth(total != null && total.getHeight() >= 30, "summary total has room for its full text");
            assertInViewport(total, workspace);
            for (String text : List.of("Edit", "Remove selected", "Clear orders", "Checkout")) {
                assertInViewport(button(shell, text), workspace);
            }
            JTable table = visible(shell, JTable.class);
            truth(table.getTableHeader().getHeight() > 0, "order table header is rendered");
        } else if (page.equals("receipt")) {
            for (String text : List.of("Open receipt", "Delete selected", "Clear cache")) {
                assertInViewport(button(shell, text), workspace);
            }
        } else if (page.equals("settings")) {
            assertInViewport(button(shell, "Reload order history"), workspace);
        }
    }

    private static void assertInViewport(Component component, JScrollPane workspace) {
        Rectangle bounds = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(),
                workspace.getViewport().getView());
        truth(workspace.getViewport().getViewRect().contains(bounds),
                "default window shows the entire control without scrolling: " + bounds);
    }

    private static boolean hasVisibleLabel(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (!child.isVisible()) { continue; }
            if (child instanceof JLabel label && text.equals(label.getText())) { return true; }
            if (child instanceof Container nested && hasVisibleLabel(nested, text)) { return true; }
        }
        return false;
    }

    private static <T extends Component> T named(Container root, String name, Class<T> type) {
        T found = find(root, type, component -> name.equals(component.getName()));
        if (found == null) { throw new AssertionError("Missing visible control: " + name); }
        return found;
    }

    private static AbstractButton button(Container root, String text) {
        AbstractButton found = find(root, AbstractButton.class, component -> text.equals(component.getText()));
        if (found == null) { throw new AssertionError("Missing visible button: " + text); }
        return found;
    }

    private static <T extends Component> T visible(Container root, Class<T> type) {
        T found = find(root, type, component -> true);
        if (found == null) { throw new AssertionError("Missing visible " + type.getSimpleName()); }
        return found;
    }

    private static <T extends Component> T find(Container root, Class<T> type, java.util.function.Predicate<T> predicate) {
        for (Component child : root.getComponents()) {
            if (!child.isVisible()) { continue; }
            if (type.isInstance(child) && predicate.test(type.cast(child))) { return type.cast(child); }
            if (child instanceof Container nested) {
                T found = find(nested, type, predicate);
                if (found != null) { return found; }
            }
        }
        return null;
    }

    private static void money(String expected, BigDecimal actual, String message) {
        truth(new BigDecimal(expected).compareTo(actual) == 0, message);
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void truth(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }

    private static final class FakeActions implements OrderActions {
        private AppShell shell;
        private int added;
        private int removedIndex = -1;
        private Order lastOrder;
        private List<Order> currentOrders;
        private int checkouts;
        private int receiptSaves;
        private int receiptExports;
        private int receiptOpens;
        private int receiptDeletes;
        private int receiptClears;
        private List<UUID> deletedReceipts;
        private Receipt lastReceipt;
        private Receipt lastOpened;
        private Receipt lastPrinted;

        @Override public boolean addOrder(Order order) { added++; lastOrder = order; return true; }
        @Override public boolean updateOrder(int index, Order order) { lastOrder = order; return true; }
        @Override public void removeOrder(int index) { removedIndex = index; }
        @Override public void clearOrders() { }
        @Override public void refreshOrders() { }
        @Override public void checkout() { checkouts++; lastReceipt = Receipt.capture(currentOrders); }
        @Override public void openReceipt(Receipt receipt) { receiptOpens++; lastOpened = receipt; }
        @Override public void refreshReceipts() { }
        @Override public void deleteReceipts(List<UUID> ids) { receiptDeletes++; deletedReceipts = List.copyOf(ids); }
        @Override public void clearReceipts() { receiptClears++; }
        @Override public boolean saveReceipt(Receipt receipt) { receiptSaves++; return true; }
        @Override public boolean exportReceipt(Receipt receipt) { receiptExports++; return true; }
        @Override public void printReceipt(Receipt receipt) { lastPrinted = receipt; }
        @Override public void setCompactRows(boolean compact) { shell.setCompactRows(compact); }
        @Override public void navigate(String page) { shell.showPage(page); }
    }
}
