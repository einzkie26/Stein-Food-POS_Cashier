package finalPPKG.ui;

import finalPPKG.app.OrderActions;
import finalPPKG.model.Receipt;
import finalPPKG.ui.views.ReceiptDialogPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** A modal window hosting the reusable receipt view. */
public final class ReceiptDialog extends JDialog {
    public ReceiptDialog(Component parent, Receipt receipt, OrderActions actions, boolean saved, boolean historyReady) {
        super(SwingUtilities.getWindowAncestor(parent), "Receipt " + receipt.getNumber(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(new ReceiptDialogPanel(receipt, actions, saved, historyReady, this::dispose));
        java.awt.Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(630, 450));
        setSize(700, Math.min(750, Math.max(450, screen.height - 80)));
        setLocationRelativeTo(parent);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { dispose(); }
        });
    }
}
