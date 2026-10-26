package finalPPKG.ui.components;

import finalPPKG.model.MenuItem;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** A responsive, single-selection menu; arrow keys move between cards. */
public final class MenuCardPicker extends JPanel {
    private static final int GAP = 10;
    private static final int MIN_CARD_WIDTH = 168;
    private final GridLayout grid = new GridLayout(0, 5, GAP, GAP);
    private final List<MenuCard> cards = new ArrayList<>();

    public MenuCardPicker(List<MenuItem> menu, Consumer<MenuItem> onSelect) {
        setLayout(grid);
        setOpaque(false);
        setName("dish");
        getAccessibleContext().setAccessibleName("Choose a dish");
        ButtonGroup group = new ButtonGroup();
        for (MenuItem item : menu) {
            MenuCard card = new MenuCard(item);
            card.addActionListener(event -> onSelect.accept(item));
            group.add(card);
            cards.add(card);
            add(card);
        }
        if (!cards.isEmpty()) { cards.get(0).setSelected(true); }
        bind(KeyEvent.VK_LEFT, "previous", -1, onSelect);
        bind(KeyEvent.VK_RIGHT, "next", 1, onSelect);
        bind(KeyEvent.VK_UP, "above", 0, onSelect);
        bind(KeyEvent.VK_DOWN, "below", 0, onSelect);
    }

    public void select(MenuItem item) {
        for (MenuCard card : cards) { if (card.getItem() == item) { card.setSelected(true); return; } }
    }

    private void bind(int key, String action, int step, Consumer<MenuItem> onSelect) {
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), action);
        getActionMap().put(action, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (cards.isEmpty()) { return; }
                int selected = 0;
                for (int index = 0; index < cards.size(); index++) {
                    if (cards.get(index).isSelected()) { selected = index; break; }
                }
                int movement = step == 0 ? (key == KeyEvent.VK_UP ? -grid.getColumns() : grid.getColumns()) : step;
                int next = Math.max(0, Math.min(cards.size() - 1, selected + movement));
                MenuCard card = cards.get(next);
                card.setSelected(true);
                card.requestFocusInWindow();
                card.scrollRectToVisible(new java.awt.Rectangle(0, 0, card.getWidth(), card.getHeight()));
                onSelect.accept(card.getItem());
            }
        });
    }

    @Override public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        int columns = Math.max(1, Math.min(5, (width + GAP) / (MIN_CARD_WIDTH + GAP)));
        if (columns != grid.getColumns()) {
            grid.setColumns(columns);
            revalidate();
        }
    }

    @Override public Dimension getPreferredSize() {
        int rows = (cards.size() + grid.getColumns() - 1) / grid.getColumns();
        return new Dimension(0, rows == 0 ? 0 : rows * (MenuCard.HEIGHT + GAP) - GAP);
    }
}
