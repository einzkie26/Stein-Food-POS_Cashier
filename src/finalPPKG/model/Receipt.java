package finalPPKG.model;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An immutable checkout snapshot, independent of later changes to the order list. */
public final class Receipt {
    private final UUID id;
    private final Instant createdAt;
    private final List<Order> orders;
    private final String exportPath;

    public Receipt(UUID id, Instant createdAt, List<Order> orders, String exportPath) {
        this.id = Objects.requireNonNull(id, "Receipt ID is required.");
        this.createdAt = Objects.requireNonNull(createdAt, "Receipt date is required.");
        this.orders = List.copyOf(orders);
        if (this.orders.isEmpty()) { throw new IllegalArgumentException("A receipt must include at least one order."); }
        this.exportPath = exportPath == null ? "" : exportPath;
    }

    public static Receipt capture(List<Order> orders) {
        return new Receipt(UUID.randomUUID(), Instant.now(), orders, "");
    }

    public UUID getId() { return id; }
    public String getNumber() { return id.toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT); }
    public Instant getCreatedAt() { return createdAt; }
    public List<Order> getOrders() { return orders; }
    public String getExportPath() { return exportPath; }
    public boolean isExported() { return !exportPath.isBlank(); }
    public String getStatus() { return isExported() ? "Exported" : "Saved"; }
    public long getItemCount() { return orders.stream().mapToLong(Order::getQuantity).sum(); }
    public BigDecimal getTotal() {
        return orders.stream().map(Order::getTotal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }

    public Receipt withExportPath(Path destination) {
        return new Receipt(id, createdAt, orders, destination.toAbsolutePath().normalize().toString());
    }
}
