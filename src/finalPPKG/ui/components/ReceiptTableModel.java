package finalPPKG.ui.components;

import finalPPKG.model.Receipt;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.swing.table.AbstractTableModel;

/** Receipt snapshots, with typed dates and totals for correct sorting. */
public final class ReceiptTableModel extends AbstractTableModel {
    public static final int SELECT_COLUMN = 0;
    private static final String[] COLUMNS = {"Select", "Receipt", "Saved on", "Items", "Total", "Status"};
    private List<Receipt> receipts = List.of();
    private final Set<UUID> selected = new HashSet<>();
    private boolean selectionEnabled;

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = List.copyOf(receipts);
        selected.clear();
        fireTableDataChanged();
    }
    public Receipt getReceipt(int index) { return receipts.get(index); }
    public List<UUID> getSelectedIds() {
        return receipts.stream().map(Receipt::getId).filter(selected::contains).toList();
    }
    public void setSelectionEnabled(boolean enabled) {
        selectionEnabled = enabled;
        if (!enabled && !selected.isEmpty()) {
            selected.clear();
            fireTableDataChanged();
        }
    }
    @Override public boolean isCellEditable(int row, int column) {
        return selectionEnabled && column == SELECT_COLUMN;
    }
    @Override public void setValueAt(Object value, int row, int column) {
        if (!isCellEditable(row, column)) { return; }
        UUID id = receipts.get(row).getId();
        if (Boolean.TRUE.equals(value)) { selected.add(id); }
        else { selected.remove(id); }
        fireTableCellUpdated(row, column);
    }
    @Override public int getRowCount() { return receipts.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }
    @Override public Class<?> getColumnClass(int column) {
        return switch (column) {
            case 0 -> Boolean.class;
            case 1, 5 -> String.class;
            case 2 -> Instant.class;
            case 3 -> Long.class;
            case 4 -> BigDecimal.class;
            default -> throw new IndexOutOfBoundsException("Unknown column: " + column);
        };
    }
    @Override public Object getValueAt(int row, int column) {
        Receipt receipt = receipts.get(row);
        return switch (column) {
            case 0 -> selected.contains(receipt.getId());
            case 1 -> receipt.getNumber();
            case 2 -> receipt.getCreatedAt();
            case 3 -> receipt.getItemCount();
            case 4 -> receipt.getTotal();
            case 5 -> receipt.getStatus();
            default -> throw new IndexOutOfBoundsException("Unknown column: " + column);
        };
    }
}

