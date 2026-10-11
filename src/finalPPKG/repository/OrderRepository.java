package finalPPKG.repository;

import finalPPKG.model.Order;
import java.io.IOException;
import java.util.List;

/** Persistence boundary: the application does not need to know how orders are stored. */
public interface OrderRepository {
    List<Order> load() throws IOException;
    void save(List<Order> orders) throws IOException;
}
