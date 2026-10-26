package finalPPKG.ui.views;

import finalPPKG.app.OrderActions;
import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.AppIcon;
import finalPPKG.ui.components.ReceiptTableModel;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

/** Clicking a history row opens the original receipt snapshot, even after orders change. */
public final class ReceiptPanel extends WorkspacePanel {
    private final ReceiptTableModel model = new ReceiptTableModel();
    private final JTable table = new JTable(model);
    private final JLabel count = UiSupport.label("No saved receipts yet.", 12, Font.PLAIN, Theme.MUTED);
    private final AppButton open = new AppButton("Open receipt", AppButton.Style.PRIMARY);
    private final AppButton delete = new AppButton("Delete selected", AppButton.Style.DANGER);
    private final AppButton clear = new AppButton("Clear cache", AppButton.Style.SECONDARY);
    private boolean historyReady;

    public ReceiptPanel(OrderActions actions) {
        super("Receipt history");
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 16));
        UiSupport.padding(card, 18);
        JPanel toolbar = UiSupport.transparent(new BorderLayout());
        toolbar.add(UiSupport.label("Saved receipts", 17, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        AppButton refresh = new AppButton("Refresh", AppButton.Style.SECONDARY);
        refresh.setIcon(new AppIcon(AppIcon.Kind.REFRESH, 16, Theme.MUTED));
        refresh.addActionListener(event -> actions.refreshReceipts());
        toolbar.add(refresh, BorderLayout.EAST);
        card.add(toolbar, BorderLayout.NORTH);
        table.setName("receiptHistory");
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(48);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(Theme.SELECTION);
        table.setSelectionForeground(Theme.TEXT);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.SURFACE);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(source, value, selected, focused, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                setHorizontalAlignment(column == 3 || column == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setForeground(!selected && column == 4 ? Theme.PRIMARY : Theme.TEXT);
                setBackground(selected ? Theme.SELECTION : row % 2 == 0 ? Theme.SURFACE : Theme.SURFACE_SOFT);
                setFont(Theme.font(14, column == 4 ? Font.BOLD : Font.PLAIN));
                if (value instanceof BigDecimal money) { setText(UiSupport.money(money)); }
                if (value instanceof Instant date) {
                    setText(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                            .withZone(ZoneId.systemDefault()).format(date));
                }
                return this;
            }
        };
        for (Class<?> type : List.of(Object.class, String.class, Long.class, BigDecimal.class, Instant.class)) {
            table.setDefaultRenderer(type, renderer);
        }
        javax.swing.table.TableCellRenderer selectionRenderer = table.getDefaultRenderer(Boolean.class);
        table.setDefaultRenderer(Boolean.class, (source, value, selected, focused, row, column) -> {
            Component component = selectionRenderer.getTableCellRendererComponent(source, value, selected, focused, row, column);
            component.setBackground(selected ? Theme.SELECTION : row % 2 == 0 ? Theme.SURFACE : Theme.SURFACE_SOFT);
            component.setEnabled(historyReady);
            if (component instanceof javax.swing.JComponent control) {
                control.setToolTipText("Select this receipt for deletion");
            }
            return component;
        });
        table.getColumnModel().getColumn(0).setMinWidth(72);
        table.getColumnModel().getColumn(0).setMaxWidth(72);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));
        javax.swing.table.TableCellRenderer originalHeader = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((source, value, selected, focused, row, column) -> {
            Component component = originalHeader.getTableCellRendererComponent(source, value, selected, focused, row, column);
            if (component instanceof JLabel label) {
                label.setOpaque(true);
                label.setBackground(Theme.SURFACE_SOFT);
                label.setForeground(Theme.MUTED);
                label.setFont(Theme.font(12, Font.BOLD));
                label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                label.setHorizontalAlignment(column == 0 ? SwingConstants.CENTER
                        : column == 3 || column == 4 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            }
            return component;
        });
        table.getAccessibleContext().setAccessibleName("Saved receipt history");
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        card.add(scroll, BorderLayout.CENTER);
        JPanel footer = UiSupport.transparent(new BorderLayout(0, 10));
        footer.add(count, BorderLayout.NORTH);
        JPanel controls = UiSupport.transparent(new BorderLayout());
        JPanel cleanup = UiSupport.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        delete.setName("deleteReceipts");
        clear.setName("clearReceiptCache");
        delete.setEnabled(false);
        clear.setEnabled(false);
        delete.addActionListener(event -> actions.deleteReceipts(model.getSelectedIds()));
        clear.addActionListener(event -> actions.clearReceipts());
        cleanup.add(delete);
        cleanup.add(clear);
        controls.add(cleanup, BorderLayout.WEST);
        controls.add(open, BorderLayout.EAST);
        footer.add(controls, BorderLayout.CENTER);
        open.setName("openReceipt");
        open.setIcon(new AppIcon(AppIcon.Kind.RECEIPT, 16, Theme.SURFACE));
        open.setEnabled(false);
        open.addActionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) { actions.openReceipt(model.getReceipt(table.convertRowIndexToModel(row))); }
        });
        table.getSelectionModel().addListSelectionListener(event -> updateControls());
        model.addTableModelListener(event -> updateControls());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                if (event.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(event) && row >= 0
                        && column >= 0 && table.convertColumnIndexToModel(column) != ReceiptTableModel.SELECT_COLUMN) {
                    actions.openReceipt(model.getReceipt(table.convertRowIndexToModel(row)));
                }
            }
        });
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
    }

    public void onReceiptsChanged(List<Receipt> receipts) {
        if (table.isEditing()) { table.getCellEditor().cancelCellEditing(); }
        table.clearSelection();
        model.setReceipts(receipts);
        updateControls();
    }
    public void setHistoryReady(boolean ready) {
        historyReady = ready;
        if (table.isEditing()) { table.getCellEditor().cancelCellEditing(); }
        model.setSelectionEnabled(ready);
        table.repaint();
        updateControls();
    }
    private void updateControls() {
        int total = model.getRowCount();
        int selected = model.getSelectedIds().size();
        count.setText(total == 0 ? "No saved receipts yet."
                : total + " saved " + (total == 1 ? "receipt" : "receipts")
                        + (selected == 0 ? "" : " · " + selected + " selected"));
        open.setEnabled(table.getSelectedRow() >= 0);
        delete.setEnabled(historyReady && selected > 0);
        clear.setEnabled(historyReady && total > 0);
    }
    @Override public void onOrdersChanged(List<Order> orders) { }
    @Override public void setCompactRows(boolean compact) { table.setRowHeight(compact ? 38 : 48); }
}

