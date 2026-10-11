package finalPPKG;

import finalPPKG.app.AppConfig;
import finalPPKG.app.OrderController;
import finalPPKG.repository.FileOrderRepository;
import finalPPKG.repository.FileReceiptRepository;
import finalPPKG.service.MenuCatalog;
import finalPPKG.service.OrderService;
import finalPPKG.service.ReceiptService;
import finalPPKG.service.ReceiptHistoryService;
import finalPPKG.ui.AppFrame;
import finalPPKG.ui.theme.Theme;
import java.awt.EventQueue;
import javax.swing.JOptionPane;

/** Original launch name retained for existing Eclipse run configurations. */
public final class finalProject_OBAOB3 {
    private finalProject_OBAOB3() { }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Theme.install();
                AppConfig config = new AppConfig();
                OrderService orders = new OrderService(new FileOrderRepository(config.getOrderFile()));
                ReceiptHistoryService history = new ReceiptHistoryService(new FileReceiptRepository(config.getReceiptHistoryDirectory()));
                OrderController controller = new OrderController(orders, new ReceiptService(), history, config);
                AppFrame frame = new AppFrame(controller, new MenuCatalog(), config);
                controller.attach(frame.getShell());
                frame.setVisible(true);
                controller.refreshOrders();
                controller.refreshReceipts();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, exception.getMessage(),
                        "Stein Food Hub could not start", JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
        });
    }
}
