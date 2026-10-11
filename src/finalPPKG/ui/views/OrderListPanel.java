package finalPPKG.ui.views;

import finalPPKG.app.OrderActions;
import finalPPKG.model.MenuItem;
import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.AppButton;
import finalPPKG.ui.components.OrderTable;
import finalPPKG.ui.components.OrderTableModel;
import finalPPKG.ui.components.RoundedPanel;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

/** The order list is a reusable part of New order, rather than another workspace. */
public final class OrderListPanel extends RoundedPanel {
    private final OrderTableModel model = new OrderTableModel();
    private final OrderTable table = new OrderTable(model);
    private final JTextField search = new JTextField(17);
    private final JLabel count = UiSupport.label("No orders yet", 12, Font.PLAIN, Theme.MUTED);
    private final AppButton edit = new AppButton("Edit", AppButton.Style.SECONDARY);
    private final AppButton remove = new AppButton("Remove selected", AppButton.Style.SECONDARY);
    private final AppButton clear = new AppButton("Clear orders", AppButton.Style.SECONDARY);
    private final AppButton checkout = new AppButton("Checkout", AppButton.Style.PRIMARY);
    private final List<MenuItem> menu;
    private final OrderActions actions;
    private boolean storageReady;

    public OrderListPanel(List<MenuItem> menu, OrderActions actions) {
        super(new BorderLayout(0, 12));
        this.menu = menu;
        this.actions = actions;
        UiSupport.padding(this, 16);
        JPanel toolbar = UiSupport.transparent(new BorderLayout(12, 0));
        toolbar.add(UiSupport.pair(UiSupport.label("Orders", 16, Font.BOLD, Theme.TEXT), count), BorderLayout.WEST);
        JPanel tools = UiSupport.transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JLabel searchLabel = UiSupport.label("Search", 12, Font.PLAIN, Theme.MUTED);
        searchLabel.setLabelFor(search);
        search.setName("searchOrders");
        search.setPreferredSize(new Dimension(210, 36));
        search.getAccessibleContext().setAccessibleName("Search dishes");
        tools.add(searchLabel);
        tools.add(search);
        AppButton refresh = new AppButton("Refresh", AppButton.Style.SECONDARY);
        refresh.addActionListener(event -> actions.refreshOrders());
        tools.add(refresh);
        toolbar.add(tools, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
        table.setName("orderList");
        table.setRowHeight(40);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.SURFACE);
        add(scroll, BorderLayout.CENTER);
        JPanel footer = UiSupport.transparent(new BorderLayout(12, 8));
        JPanel buttons = UiSupport.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(edit);
        buttons.add(remove);
        buttons.add(clear);
        JPanel actionRow = UiSupport.transparent(new BorderLayout());
        actionRow.add(buttons, BorderLayout.WEST);
        actionRow.add(checkout, BorderLayout.EAST);
        checkout.setName("checkout");
        footer.add(actionRow, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { filter(); }
            public void removeUpdate(DocumentEvent event) { filter(); }
            public void changedUpdate(DocumentEvent event) { filter(); }
        });
        table.getSelectionModel().addListSelectionListener(event -> updateSelection());
        table.getRowSorter().addRowSorterListener(event -> updateCount());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event)
                        && table.rowAtPoint(event.getPoint()) >= 0) { editSelected(); }
            }
        });
        edit.addActionListener(event -> editSelected());
        remove.addActionListener(event -> {
            int index = selectedIndex();
            if (storageReady && index >= 0) { actions.removeOrder(index); }
        });
        clear.addActionListener(event -> { if (storageReady) { actions.clearOrders(); } });
        checkout.addActionListener(event -> {
            if (storageReady && model.getRowCount() > 0) { actions.checkout(); }
        });
        setStorageReady(false);
    }

    private int selectedIndex() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? -1 : table.convertRowIndexToModel(viewRow);
    }

    private void editSelected() {
        if (!storageReady) { return; }
        int index = selectedIndex();
        if (index < 0) { return; }
        OrderFormPanel form = new OrderFormPanel(menu, model.getOrder(index));
        form.setPreferredSize(new Dimension(390, 220));
        while (JOptionPane.showConfirmDialog(this, form, "Edit order",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                if (actions.updateOrder(index, form.readOrder())) { return; }
            } catch (IllegalArgumentException exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Check order details", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void filter() {
        TableRowSorter<OrderTableModel> sorter = (TableRowSorter<OrderTableModel>) table.getRowSorter();
        String query = search.getText().trim();
        sorter.setRowFilter(query.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(query), 0));
        updateCount();
        updateSelection();
    }

    private void updateCount() {
        int visible = table.getRowCount();
        count.setText(model.getRowCount() == 0 ? "No orders yet."
                : visible == 0 ? "No dishes match your search."
                : visible + " of " + model.getRowCount() + " orders");
    }

    private void updateSelection() {
        boolean selected = storageReady && table.getSelectedRow() >= 0;
        edit.setEnabled(selected);
        remove.setEnabled(selected);
    }

    public void setOrders(List<Order> orders) {
        table.clearSelection();
        model.setOrders(orders);
        clear.setEnabled(storageReady && !orders.isEmpty());
        checkout.setEnabled(storageReady && !orders.isEmpty());
        updateCount();
        updateSelection();
    }

    public void setCompactRows(boolean compact) {
        table.setCompact(compact);
        table.setRowHeight(compact ? 32 : 40);
    }

    public void setStorageReady(boolean ready) {
        storageReady = ready;
        clear.setEnabled(ready && model.getRowCount() > 0);
        checkout.setEnabled(ready && model.getRowCount() > 0);
        updateSelection();
    }
}

