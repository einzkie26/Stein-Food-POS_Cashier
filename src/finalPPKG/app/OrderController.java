package finalPPKG.app;

import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import finalPPKG.service.OrderService;
import finalPPKG.service.ReceiptService;
import finalPPKG.service.ReceiptHistoryService;
import finalPPKG.ui.AppShell;
import finalPPKG.ui.ReceiptDialog;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Coordinates order entry, receipt snapshots and modal actions. */
public final class OrderController implements OrderActions {
    private final OrderService orders;
    private final ReceiptService receipts;
    private final ReceiptHistoryService history;
    private final AppConfig config;
    private AppShell shell;
    private boolean historyLoaded;
    private boolean receiptsLoaded;

    public OrderController(OrderService orders, ReceiptService receipts, ReceiptHistoryService history, AppConfig config) {
        this.orders = orders;
        this.receipts = receipts;
        this.history = history;
        this.config = config;
    }

    public void attach(AppShell shell) { this.shell = shell; }

    @Override public boolean addOrder(Order order) {
        return mutate(() -> orders.add(order), "Order added · " + order.getDish());
    }

    @Override public boolean updateOrder(int index, Order order) {
        return mutate(() -> orders.update(index, order), "Order updated · " + order.getDish());
    }

    @Override public void removeOrder(int index) {
        if (!canWrite() || index < 0 || index >= orders.getOrders().size()) { return; }
        Order order = orders.getOrders().get(index);
        if (JOptionPane.showConfirmDialog(parent(), "Remove " + order.getDish() + " from the order list?",
                "Remove order", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
            mutate(() -> orders.remove(index), "Order removed.");
        }
    }

    @Override public void clearOrders() {
        if (!canWrite() || orders.getOrders().isEmpty()) { return; }
        if (JOptionPane.showConfirmDialog(parent(), "Clear all current orders? Saved receipts will remain in history.",
                "Clear orders", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
            mutate(orders::clear, "Current orders cleared.");
        }
    }

    private boolean mutate(StorageAction action, String success) {
        if (!canWrite()) { return false; }
        try {
            action.run();
            shell.updateOrders(orders.getOrders());
            shell.setStatus(success, false);
            return true;
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException exception) {
            showError("Could not save orders", exception);
            return false;
        }
    }

    private boolean canWrite() {
        if (!historyLoaded) {
            shell.setStatus("Order history is unavailable. Fix the file and use Refresh to try again.", true);
        }
        return historyLoaded;
    }

    @Override public void refreshOrders() {
        try {
            orders.refresh();
            historyLoaded = true;
            shell.updateOrders(orders.getOrders());
            shell.setStorageReady(true);
            shell.setStatus(orders.getOrders().isEmpty() ? "Ready for your first order."
                    : "Order list is up to date.", false);
        } catch (IOException exception) {
            historyLoaded = false;
            shell.setStorageReady(false);
            showError("Could not load orders", exception);
        }
    }

    @Override public void refreshReceipts() {
        try {
            history.refresh();
            receiptsLoaded = true;
            shell.updateReceipts(history.getReceipts());
            shell.setReceiptHistoryReady(true);
            shell.setStatus("Receipt history is up to date.", false);
        } catch (IOException exception) {
            receiptsLoaded = false;
            shell.setReceiptHistoryReady(false);
            showError("Could not load receipt history", exception);
        }
    }

    @Override public void deleteReceipts(List<UUID> ids) {
        if (!canSaveReceipts() || ids.isEmpty()) { return; }
        List<UUID> selected = List.copyOf(ids).stream().distinct().toList();
        if (JOptionPane.showConfirmDialog(parent(),
                "Delete " + selected.size() + " selected " + (selected.size() == 1 ? "receipt" : "receipts")
                        + " from history?\nExported files and current orders will remain.",
                "Delete receipts", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
            changeReceiptHistory(() -> history.deleteSelected(selected), "Selected receipts deleted.");
        }
    }

    @Override public void clearReceipts() {
        if (!canSaveReceipts() || history.getReceipts().isEmpty()) { return; }
        if (JOptionPane.showConfirmDialog(parent(),
                "Clear all " + history.getReceipts().size() + " saved receipts from the cache?\n"
                        + "Exported files and current orders will remain.",
                "Clear receipt cache", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
            changeReceiptHistory(history::clear, "Receipt cache cleared.");
        }
    }

    private void changeReceiptHistory(StorageAction action, String success) {
        try {
            action.run();
            shell.updateReceipts(history.getReceipts());
            shell.setStatus(success, false);
        } catch (IOException exception) {
            receiptsLoaded = false;
            shell.updateReceipts(history.getReceipts());
            shell.setReceiptHistoryReady(false);
            showError("Could not remove receipts", exception);
        }
    }

    @Override public void checkout() {
        if (!canWrite() || orders.getOrders().isEmpty()) { return; }
        openReceipt(Receipt.capture(orders.getOrders()));
    }

    @Override public void openReceipt(Receipt receipt) {
        boolean saved = history.getReceipts().stream().anyMatch(item -> item.getId().equals(receipt.getId()));
        new ReceiptDialog(shell, receipt, this, saved, receiptsLoaded).setVisible(true);
    }

    @Override public boolean saveReceipt(Receipt receipt) {
        if (!canSaveReceipts()) { return false; }
        try {
            history.save(receipt);
            shell.updateReceipts(history.getReceipts());
            shell.setStatus("Receipt saved · " + receipt.getNumber(), false);
            return true;
        } catch (IOException exception) {
            showError("Could not save receipt", exception);
            return false;
        }
    }

    private boolean canSaveReceipts() {
        if (!receiptsLoaded) {
            shell.setStatus("Receipt history is unavailable. Use Refresh on the Receipts page to reload it.", true);
        }
        return receiptsLoaded;
    }

    @Override public boolean exportReceipt(Receipt receipt) {
        if (!canSaveReceipts()) { return false; }
        JFileChooser chooser = new JFileChooser(config.getDataDirectory().toFile());
        chooser.setDialogTitle("Export receipt");
        chooser.setFileFilter(new FileNameExtensionFilter("Text receipt (*.txt)", "txt"));
        chooser.setSelectedFile(config.getDataDirectory().resolve("receipt-" + receipt.getNumber() + ".txt").toFile());
        if (chooser.showSaveDialog(parent()) != JFileChooser.APPROVE_OPTION) { return false; }
        Path selected = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
        if (!selected.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt")) {
            selected = selected.resolveSibling(selected.getFileName() + ".txt");
        }
        boolean exported = false;
        try {
            if (selected.equals(config.getOrderFile()) || selected.startsWith(config.getReceiptHistoryDirectory())
                    || (Files.exists(selected) && Files.exists(config.getOrderFile())
                    && Files.isSameFile(selected, config.getOrderFile()))) {
                throw new IOException("Choose an export location outside the application's history files.");
            }
            if (Files.exists(selected) && JOptionPane.showConfirmDialog(parent(),
                    "Replace the existing receipt at " + selected + "?", "Replace receipt",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) { return false; }
            // Archive first: a completed export always has a saved snapshot to reopen.
            Receipt saved = history.save(receipt);
            shell.updateReceipts(history.getReceipts());
            receipts.export(saved, selected);
            exported = true;
            history.markExported(saved, selected);
            shell.updateReceipts(history.getReceipts());
            shell.setStatus("Receipt exported · " + selected, false);
            return true;
        } catch (IOException exception) {
            if (exported) {
                showError("Receipt exported, but history status could not be updated", exception);
                return true;
            }
            showError("Could not export receipt", exception);
            return false;
        }
    }

    @Override public void printReceipt(Receipt receipt) {
        JTextArea text = new JTextArea(receipts.format(receipt));
        text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        try {
            if (text.print()) { shell.setStatus("Receipt sent to your printer.", false); }
        } catch (PrinterException exception) { showError("Could not print receipt", exception); }
    }

    @Override public void setCompactRows(boolean compact) {
        shell.setCompactRows(compact);
        shell.setStatus(compact ? "Compact table rows enabled." : "Comfortable table rows enabled.", false);
    }

    @Override public void navigate(String page) { shell.showPage(page); }

    private Component parent() {
        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        return active != null && active.isVisible() ? active : shell;
    }

    private void showError(String title, Exception exception) {
        shell.setStatus(title + " · " + exception.getMessage(), true);
        JOptionPane.showMessageDialog(parent(), exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface private interface StorageAction { void run() throws IOException; }
}

