package finalPPKG.ui.components;

import finalPPKG.model.Order;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Typed table data; formatting stays in OrderTable so numeric sorting works. */
public final class OrderTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Dish", "Quantity", "Unit price", "Discount", "Total"};
    private List<Order> orders = List.of();

    public void setOrders(List<Order> orders) {
        this.orders = List.copyOf(orders);
        fireTableDataChanged();
    }

    public Order getOrder(int modelIndex) {
        return orders.get(modelIndex);
    }

    @Override
    public int getRowCount() {
        return orders.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 0: return String.class;
            case 1: return Integer.class;
            case 2:
            case 3:
            case 4: return BigDecimal.class;
            default: throw new IndexOutOfBoundsException("Unknown column: " + column);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Order order = getOrder(rowIndex);
        switch (columnIndex) {
            case 0: return order.getDish();
            case 1: return order.getQuantity();
            case 2: return order.getUnitPrice();
            case 3: return order.getDiscountPercent();
            case 4: return order.getTotal();
            default: throw new IndexOutOfBoundsException("Unknown column: " + columnIndex);
        }
    }
}
