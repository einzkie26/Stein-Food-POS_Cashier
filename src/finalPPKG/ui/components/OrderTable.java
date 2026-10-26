package finalPPKG.ui.components;

import finalPPKG.ui.theme.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/** Readable order table with numeric sorting and PHP amounts. */
public final class OrderTable extends JTable {
    private static final long serialVersionUID = 1L;
    private static final Color ALTERNATE_ROW = Theme.SURFACE_SOFT;
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public OrderTable(OrderTableModel model) {
        super(model);
        setFont(Theme.font(14, Font.PLAIN));
        setForeground(Theme.TEXT);
        setBackground(Theme.SURFACE);
        setSelectionBackground(Theme.SELECTION);
        setSelectionForeground(Theme.TEXT);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoCreateRowSorter(true);
        setFillsViewportHeight(true);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setRowHeight(52);
        setBorder(BorderFactory.createEmptyBorder());
        setDefaultRenderer(Object.class, new OrderCellRenderer());
        setDefaultRenderer(String.class, new OrderCellRenderer());
        setDefaultRenderer(Integer.class, new OrderCellRenderer());
        setDefaultRenderer(BigDecimal.class, new OrderCellRenderer());
        getColumnModel().getColumn(0).setPreferredWidth(260);
        getColumnModel().getColumn(1).setPreferredWidth(90);
        getColumnModel().getColumn(2).setPreferredWidth(145);
        getColumnModel().getColumn(3).setPreferredWidth(100);
        getColumnModel().getColumn(4).setPreferredWidth(150);
        for (int column = 0; column < getColumnCount(); column++) {
            getColumnModel().getColumn(column).setMinWidth(column == 0 ? 150 : 78);
        }

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setBackground(Theme.SURFACE_SOFT);
        header.setForeground(Theme.MUTED);
        header.setFont(Theme.font(12, Font.BOLD));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        TableCellRenderer originalHeaderRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((table, value, selected, focused, row, column) -> {
            Component component = originalHeaderRenderer.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (component instanceof javax.swing.JLabel label) {
                label.setOpaque(true);
                label.setBackground(Theme.SURFACE_SOFT);
                label.setForeground(Theme.MUTED);
                label.setFont(Theme.font(12, Font.BOLD));
                label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            }
            return component;
        });
        getAccessibleContext().setAccessibleName("Order items");
        getAccessibleContext().setAccessibleDescription("Select an order item to edit or remove it. Click a column header to sort.");
    }

    public void setCompact(boolean compact) {
        setRowHeight(compact ? 42 : 52);
        setFont(Theme.font(compact ? 13 : 14, Font.PLAIN));
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        Point point = event.getPoint();
        int row = rowAtPoint(point);
        int column = columnAtPoint(point);
        if (row < 0 || column < 0) {
            return null;
        }
        Object value = getValueAt(row, column);
        return value == null ? null : value.toString();
    }

    private final class OrderCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            int modelColumn = table.convertColumnIndexToModel(column);
            setFont(modelColumn == 4 ? table.getFont().deriveFont(Font.BOLD) : table.getFont());
            setForeground(modelColumn == 4 && !selected ? Theme.PRIMARY : Theme.TEXT);
            setBackground(selected ? Theme.SELECTION : row % 2 == 0 ? Theme.SURFACE : ALTERNATE_ROW);
            setHorizontalAlignment(modelColumn == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
            setBorder(focused
                    ? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.PRIMARY),
                            BorderFactory.createEmptyBorder(0, 11, 0, 11))
                    : BorderFactory.createEmptyBorder(0, 12, 0, 12));
            if (value instanceof BigDecimal decimal) {
                setText(modelColumn == 3 ? decimal.stripTrailingZeros().toPlainString() + "%"
                        : "PHP " + money.format(decimal));
            }
            return this;
        }
    }
}
