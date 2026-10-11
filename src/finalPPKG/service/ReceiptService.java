package finalPPKG.service;

import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Plain-text receipts that can be viewed, printed, or exported without UI dependencies. */
public final class ReceiptService {
    public String format(Receipt receipt) {
        String date = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault()).format(receipt.getCreatedAt());
        return "Receipt: " + receipt.getNumber() + "\nDate: " + date + "\n\n" + format(receipt.getOrders());
    }

    public String format(List<Order> orders) {
        List<Order> snapshot = List.copyOf(Objects.requireNonNull(orders, "Orders are required."));
        StringBuilder receipt = new StringBuilder();
        receipt.append("STEIN FOOD HUB\nORDER RECEIPT\n");
        receipt.append("------------------------------------------------\n");
        BigDecimal subtotal = new BigDecimal("0.00");
        BigDecimal discounts = new BigDecimal("0.00");
        BigDecimal total = new BigDecimal("0.00");
        long quantity = 0;
        for (int i = 0; i < snapshot.size(); i++) {
            Order order = snapshot.get(i);
            receipt.append(i + 1).append(". ").append(order.getDish()).append('\n');
            receipt.append("   ").append(order.getQuantity()).append(" x PHP ")
                    .append(order.getUnitPrice().toPlainString()).append('\n');
            receipt.append("   Subtotal: PHP ").append(order.getSubtotal().toPlainString()).append('\n');
            receipt.append("   Discount (").append(order.getDiscountPercent().toPlainString())
                    .append("%): PHP ").append(order.getDiscountAmount().toPlainString()).append('\n');
            receipt.append("   Line total: PHP ").append(order.getTotal().toPlainString()).append("\n\n");
            subtotal = subtotal.add(order.getSubtotal());
            discounts = discounts.add(order.getDiscountAmount());
            total = total.add(order.getTotal());
            quantity += order.getQuantity();
        }
        if (snapshot.isEmpty()) receipt.append("No orders yet.\n");
        receipt.append("------------------------------------------------\n");
        receipt.append("Order lines: ").append(snapshot.size()).append(" | Items: ").append(quantity).append('\n');
        receipt.append("Subtotal:     PHP ").append(subtotal.toPlainString()).append('\n');
        receipt.append("Discounts:    PHP ").append(discounts.toPlainString()).append('\n');
        receipt.append("TOTAL:        PHP ").append(total.toPlainString()).append('\n');
        receipt.append("\nThank you for ordering!\n");
        return receipt.toString();
    }

    public void export(List<Order> orders, Path destination) throws IOException {
        write(format(orders), destination);
    }

    public void export(Receipt receipt, Path destination) throws IOException {
        write(format(receipt), destination);
    }

    private void write(String receipt, Path destination) throws IOException {
        Path file = Objects.requireNonNull(destination, "Receipt destination is required.")
                .toAbsolutePath().normalize();
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "receipt-export-", ".tmp");
        try {
            Files.writeString(temporary, receipt, StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
