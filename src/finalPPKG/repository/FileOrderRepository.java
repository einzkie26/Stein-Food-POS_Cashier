package finalPPKG.repository;

import finalPPKG.model.Order;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** UTF-8 storage compatible with the original dish:quantity:price:discount:total file. */
public final class FileOrderRepository implements OrderRepository {
    private final Path file;

    public FileOrderRepository(Path file) {
        this.file = Objects.requireNonNull(file, "Order file is required.").toAbsolutePath().normalize();
    }

    public Path getPath() { return file; }

    @Override
    public List<Order> load() throws IOException {
        List<Order> orders = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isBlank()) continue;
                String[] fields = line.split(":", -1);
                if (fields.length != 5) {
                    throw invalidLine(lineNumber, "Expected 5 colon-separated fields.", null);
                }
                try {
                    int quantity = Integer.parseInt(fields[1].strip());
                    BigDecimal price = new BigDecimal(fields[2].strip());
                    BigDecimal discount = new BigDecimal(fields[3].strip());
                    // Validate the legacy total, but recompute it from authoritative inputs.
                    BigDecimal storedTotal = new BigDecimal(fields[4].strip());
                    if (storedTotal.signum() < 0) {
                        throw new IllegalArgumentException("Stored total cannot be negative.");
                    }
                    orders.add(new Order(fields[0], quantity, price, discount));
                } catch (IllegalArgumentException ex) {
                    throw invalidLine(lineNumber, ex.getMessage(), ex);
                }
            }
        } catch (NoSuchFileException ex) {
            return List.of();
        }
        return List.copyOf(orders);
    }

    private IOException invalidLine(int lineNumber, String reason, Throwable cause) {
        return new IOException("Cannot read " + file.getFileName() + " at line " + lineNumber
                + ": " + reason, cause);
    }

    @Override
    public void save(List<Order> orders) throws IOException {
        List<Order> snapshot = List.copyOf(Objects.requireNonNull(orders, "Orders are required."));
        Path directory = file.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "orders-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                for (Order order : snapshot) {
                    writer.write(order.getDish() + ":" + order.getQuantity() + ":"
                            + order.getUnitPrice().toPlainString() + ":"
                            + order.getDiscountPercent().toPlainString() + ":"
                            + order.getTotal().toPlainString());
                    writer.newLine();
                }
            }
            // If the filesystem cannot replace atomically, fail and retain the existing file.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
