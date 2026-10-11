package finalPPKG.ui.views;

import finalPPKG.model.Order;
import finalPPKG.ui.UiSupport;
import finalPPKG.ui.theme.Theme;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.JViewport;

/** Shared workspace layout; each screen decides how to display an order snapshot. */
public abstract class WorkspacePanel extends JPanel implements Scrollable {
    protected final JPanel body = UiSupport.transparent(new BorderLayout(18, 18));

    protected WorkspacePanel(String title) {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        JPanel heading = UiSupport.transparent(new BorderLayout(0, 5));
        heading.add(UiSupport.label(title, 27, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        add(heading, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public abstract void onOrdersChanged(List<Order> orders);

    public void setCompactRows(boolean compact) { }

    public void setStorageReady(boolean ready) { }

    @Override public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, 580);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int direction, int orientation) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int direction, int orientation) { return 100; }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof JViewport viewport && viewport.getHeight() >= getPreferredSize().height;
    }
}
