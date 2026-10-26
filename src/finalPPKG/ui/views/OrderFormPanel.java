package finalPPKG.ui.views;

import finalPPKG.model.MenuItem;
import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.components.MenuCardPicker;
import finalPPKG.ui.components.MenuIcons;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** One validated form is reused when creating and editing orders. */
public final class OrderFormPanel extends JPanel {
    private final JComboBox<MenuItem> dish = new JComboBox<>();
    private final JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
    private final JTextField unitPrice = new JTextField();
    private final JTextField discount = new JTextField("0");
    private Runnable changeListener = () -> { };
    private MenuCardPicker menuCards;

    public OrderFormPanel(List<MenuItem> menu, Order initial) {
        this(menu, initial, null);
    }

    /** New orders use cards and a live summary; the edit dialog retains a compact dropdown. */
    public OrderFormPanel(List<MenuItem> menu, Order initial, JComponent summary) {
        super(new GridBagLayout());
        setOpaque(false);
        for (MenuItem item : menu) { dish.addItem(item); }
        if (initial != null) {
            MenuItem selected = menu.stream().filter(item -> item.getName().equals(initial.getDish()))
                    .findFirst().orElse(new MenuItem(initial.getDish(), "Saved dish"));
            boolean found = menu.stream().anyMatch(item -> item.getName().equals(initial.getDish()));
            if (!found) { dish.addItem(selected); }
            dish.setSelectedItem(selected);
            quantity.setValue(initial.getQuantity());
            unitPrice.setText(initial.getUnitPrice().toPlainString());
            discount.setText(initial.getDiscountPercent().stripTrailingZeros().toPlainString());
        }
        dish.setName("dish");
        quantity.setName("quantity");
        quantity.setEditor(new JSpinner.NumberEditor(quantity, "0"));
        Theme.styleComboBox(dish);
        Theme.styleSpinner(quantity);
        dish.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                label.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
                label.setFont(Theme.font(14, Font.PLAIN));
                label.setBackground(selected ? Theme.SELECTION : Theme.SURFACE);
                label.setForeground(Theme.TEXT);
                if (value instanceof MenuItem item) {
                    label.setIcon(MenuIcons.dish(item, 22));
                    label.setIconTextGap(10);
                    label.setText(item.getName());
                }
                return label;
            }
        });
        unitPrice.setName("unitPrice");
        discount.setName("discount");
        for (javax.swing.JComponent control : List.of(dish, quantity, unitPrice, discount)) {
            control.setPreferredSize(new Dimension(100, Theme.CONTROL_HEIGHT));
            control.setMinimumSize(new Dimension(64, Theme.CONTROL_HEIGHT));
        }
        dish.setPreferredSize(new Dimension(260, Theme.CONTROL_HEIGHT));
        unitPrice.setPreferredSize(new Dimension(130, Theme.CONTROL_HEIGHT));
        unitPrice.setHorizontalAlignment(JTextField.LEFT);
        unitPrice.setToolTipText("Enter the price of one portion in PHP.");
        discount.setToolTipText("Enter a percentage from 0 to 100. Leave blank for no discount.");
        quantity.setToolTipText("Number of portions, from 1 to 9,999.");
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 0, 14, 0);
        if (summary == null) { fields.add(UiSupport.field("Choose a dish", dish), constraints); }
        constraints.gridy = summary == null ? 1 : 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.75;
        constraints.insets = new Insets(0, 0, 0, 6);
        fields.add(UiSupport.field("Quantity", quantity), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.25;
        constraints.insets = new Insets(0, 6, 0, 6);
        fields.add(UiSupport.field("Unit price (PHP)", unitPrice), constraints);
        constraints.gridx = 2;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 6, 0, 0);
        fields.add(UiSupport.field("Discount (%)", discount), constraints);
        setLayout(new BorderLayout(0, 12));
        if (summary == null) {
            add(fields, BorderLayout.CENTER);
        } else {
            menuCards = new MenuCardPicker(menu, item -> {
                if (dish.getSelectedItem() != item) { dish.setSelectedItem(item); }
            });
            menuCards.select((MenuItem) dish.getSelectedItem());
            add(menuCards, BorderLayout.NORTH);
            JPanel details = UiSupport.transparent(new java.awt.GridLayout(1, 2, 16, 0));
            details.add(fields);
            details.add(summary);
            add(details, BorderLayout.CENTER);
        }
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { changeListener.run(); }
            public void removeUpdate(DocumentEvent event) { changeListener.run(); }
            public void changedUpdate(DocumentEvent event) { changeListener.run(); }
        };
        unitPrice.getDocument().addDocumentListener(listener);
        discount.getDocument().addDocumentListener(listener);
        ((JSpinner.DefaultEditor) quantity.getEditor()).getTextField().getDocument().addDocumentListener(listener);
        quantity.addChangeListener(event -> changeListener.run());
        dish.addActionListener(event -> {
            if (menuCards != null) { menuCards.select((MenuItem) dish.getSelectedItem()); }
            unitPrice.setText("");
            changeListener.run();
        });
    }

    public void setChangeListener(Runnable listener) { changeListener = listener; }

    public Order readOrder() {
        MenuItem item = (MenuItem) dish.getSelectedItem();
        if (item == null) { throw new IllegalArgumentException("Choose a dish first."); }
        String quantityText = ((JSpinner.DefaultEditor) quantity.getEditor()).getTextField().getText().trim();
        int portionCount;
        try {
            if (!quantityText.matches("[0-9]+")) { throw new NumberFormatException(); }
            portionCount = Integer.parseInt(quantityText);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Quantity must be a whole number from 1 to 9,999.");
        }
        if (unitPrice.getText().isBlank()) {
            throw new IllegalArgumentException("Enter a unit price to complete the order.");
        }
        BigDecimal price = parseDecimal(unitPrice.getText(), "Unit price");
        BigDecimal percent = discount.getText().isBlank() ? BigDecimal.ZERO
                : parseDecimal(discount.getText(), "Discount");
        return new Order(item.getName(), portionCount, price, percent);
    }

    private BigDecimal parseDecimal(String input, String label) {
        String value = input.trim();
        if (!value.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)")) {
            throw new IllegalArgumentException(label + " must be a decimal number, such as 85.50.");
        }
        try { return new BigDecimal(value); }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a valid decimal number.");
        }
    }

    public void reset() {
        unitPrice.setText("");
        quantity.setValue(1);
        ((JSpinner.DefaultEditor) quantity.getEditor()).getTextField().setValue(1);
        discount.setText("0");
        unitPrice.requestFocusInWindow();
    }
}
