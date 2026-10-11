package finalPPKG.repository;

import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

/** Each receipt has its own UTF-8 file; atomic replacement protects saved snapshots. */
public final class FileReceiptRepository implements ReceiptRepository {
    private final Path directory;

    public FileReceiptRepository(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    @Override public List<Receipt> load() throws IOException {
        if (Files.notExists(directory)) { return List.of(); }
        List<Receipt> receipts = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".receipt")).sorted().toList()) {
                receipts.add(read(file));
            }
        }
        receipts.sort(Comparator.comparing(Receipt::getCreatedAt).reversed());
        return List.copyOf(receipts);
    }

    private Receipt read(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
            if (!"1".equals(required(properties, "version"))) {
                throw new IllegalArgumentException("Unsupported receipt version.");
            }
            UUID id = UUID.fromString(required(properties, "id"));
            if (!file.getFileName().toString().equals(id + ".receipt")) {
                throw new IllegalArgumentException("Receipt ID does not match its file name.");
            }
            Instant createdAt = Instant.parse(required(properties, "createdAt"));
            int count = Integer.parseInt(required(properties, "orderCount"));
            if (count < 1 || count > 100_000) { throw new IllegalArgumentException("Invalid receipt order count."); }
            List<Order> orders = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                String prefix = "order." + index + ".";
                orders.add(new Order(required(properties, prefix + "dish"),
                        Integer.parseInt(required(properties, prefix + "quantity")),
                        new BigDecimal(required(properties, prefix + "unitPrice")),
                        new BigDecimal(required(properties, prefix + "discount"))));
            }
            return new Receipt(id, createdAt, orders, properties.getProperty("exportPath", ""));
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new IOException("Cannot read receipt " + file.getFileName() + ": " + exception.getMessage(), exception);
        }
    }

    private String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) { throw new IllegalArgumentException("Missing field: " + key); }
        return value;
    }

    @Override public void save(Receipt receipt) throws IOException {
        if (receipt.getOrders().size() > 100_000) { throw new IOException("A receipt can contain at most 100,000 order lines."); }
        Properties properties = new Properties();
        properties.setProperty("version", "1");
        properties.setProperty("id", receipt.getId().toString());
        properties.setProperty("createdAt", receipt.getCreatedAt().toString());
        properties.setProperty("exportPath", receipt.getExportPath());
        properties.setProperty("orderCount", Integer.toString(receipt.getOrders().size()));
        for (int index = 0; index < receipt.getOrders().size(); index++) {
            Order order = receipt.getOrders().get(index);
            String prefix = "order." + index + ".";
            properties.setProperty(prefix + "dish", order.getDish());
            properties.setProperty(prefix + "quantity", Integer.toString(order.getQuantity()));
            properties.setProperty(prefix + "unitPrice", order.getUnitPrice().toPlainString());
            properties.setProperty(prefix + "discount", order.getDiscountPercent().toPlainString());
        }
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "receipt-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                properties.store(writer, "Stein Food Hub receipt snapshot");
            }
            Files.move(temporary, directory.resolve(receipt.getId() + ".receipt"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override public void delete(UUID id) throws IOException {
        // A UUID resolves only to this archive's own file, never an exported receipt.
        Path file = directory.resolve(java.util.Objects.requireNonNull(id, "Receipt ID") + ".receipt");
        if (Files.notExists(file, LinkOption.NOFOLLOW_LINKS)) { return; }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Receipt archive is not a regular file: " + file.getFileName());
        }
        Files.deleteIfExists(file);
    }
}
