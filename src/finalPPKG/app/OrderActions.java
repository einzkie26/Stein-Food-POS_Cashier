package finalPPKG.app;

import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import java.util.List;
import java.util.UUID;

/** Views request actions without depending on storage or the concrete controller. */
public interface OrderActions {
    boolean addOrder(Order order);
    boolean updateOrder(int index, Order order);
    void removeOrder(int index);
    void clearOrders();
    void refreshOrders();
    void checkout();
    void openReceipt(Receipt receipt);
    void refreshReceipts();
    void deleteReceipts(List<UUID> ids);
    void clearReceipts();
    boolean saveReceipt(Receipt receipt);
    boolean exportReceipt(Receipt receipt);
    void printReceipt(Receipt receipt);
    void setCompactRows(boolean compact);
    void navigate(String page);
}
