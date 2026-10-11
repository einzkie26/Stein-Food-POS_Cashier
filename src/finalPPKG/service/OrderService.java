package finalPPKG.service;

import finalPPKG.model.Order;
import finalPPKG.repository.OrderRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Order operations commit to storage before publishing a new immutable snapshot. */
public final class OrderService {
    private final OrderRepository repository;
    private List<Order> orders = List.of();
    private boolean historyLoaded;

    public OrderService(OrderRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Order repository is required.");
    }

    public List<Order> getOrders() { return orders; }

    public void refresh() throws IOException {
        historyLoaded = false;
        orders = List.copyOf(repository.load());
        historyLoaded = true;
    }

    public void add(Order order) throws IOException {
        requireLoadedHistory();
        List<Order> changed = new ArrayList<>(orders);
        changed.add(Objects.requireNonNull(order, "Order is required."));
        commit(changed);
    }

    public void update(int index, Order order) throws IOException {
        requireLoadedHistory();
        List<Order> changed = new ArrayList<>(orders);
        changed.set(index, Objects.requireNonNull(order, "Order is required."));
        commit(changed);
    }

    public void remove(int index) throws IOException {
        requireLoadedHistory();
        List<Order> changed = new ArrayList<>(orders);
        changed.remove(index);
        commit(changed);
    }

    public void clear() throws IOException {
        requireLoadedHistory();
        commit(List.of());
    }

    private void requireLoadedHistory() throws IOException {
        if (!historyLoaded) {
            throw new IOException("Load order history successfully before saving changes. Use Refresh to try again.");
        }
    }

    private void commit(List<Order> changed) throws IOException {
        List<Order> snapshot = List.copyOf(changed);
        repository.save(snapshot);
        orders = snapshot;
    }

    public BigDecimal total() {
        return orders.stream().map(Order::getTotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    public int itemCount() {
        return orders.stream().mapToInt(Order::getQuantity).sum();
    }
}
