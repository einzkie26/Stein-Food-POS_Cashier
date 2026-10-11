package finalPPKG.tests;

import finalPPKG.model.Order;
import finalPPKG.model.Receipt;
import finalPPKG.repository.FileOrderRepository;
import finalPPKG.repository.FileReceiptRepository;
import finalPPKG.repository.ReceiptRepository;
import finalPPKG.service.OrderService;
import finalPPKG.service.ReceiptHistoryService;
import finalPPKG.service.ReceiptService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Verifies durable receipt snapshots and recovery without touching real customer files. */
public final class ReceiptHistoryTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("stein-receipt-tests-").toAbsolutePath().normalize();
        try {
            survivesOrderChanges(root.resolve("snapshots"));
            roundTripAndExport(root.resolve("round-trip"));
            noDuplicateOrDowngrade(root.resolve("duplicates"));
            failedWrites();
            malformedHistory(root.resolve("malformed"));
            failedExport(root.resolve("export-failure"));
            deleteSelectedAndRestart(root.resolve("delete"));
            clearCache(root.resolve("clear"));
            failedDeletes();
            protectedDeletionTarget(root.resolve("protected-delete"));
            System.out.println("PASS: 10 receipt history checks (snapshots, export, deletion, cache clearing, and failures).");
        } finally {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.toAbsolutePath().normalize().startsWith(root)) { throw new IOException("Invalid test cleanup path."); }
                    Files.delete(path);
                }
            }
        }
    }

    private static void survivesOrderChanges(Path directory) throws Exception {
        OrderService orders = new OrderService(new FileOrderRepository(directory.resolve("orders.txt")));
        orders.refresh();
        orders.add(order("Rice", 2, "5.00"));
        ReceiptHistoryService history = new ReceiptHistoryService(new FileReceiptRepository(directory.resolve("receipts")));
        history.refresh();
        Receipt saved = Receipt.capture(orders.getOrders());
        history.save(saved);
        orders.update(0, order("Rice", 9, "10.00"));
        orders.clear();
        ReceiptHistoryService restarted = new ReceiptHistoryService(new FileReceiptRepository(directory.resolve("receipts")));
        restarted.refresh();
        Receipt original = restarted.getReceipts().get(0);
        truth(original.getId().equals(saved.getId()), "receipt ID survives restart");
        truth(original.getOrders().get(0).getQuantity() == 2, "order edits cannot change archived quantities");
        money("10.00", original.getTotal(), "clearing orders cannot change receipt total");
        expect(UnsupportedOperationException.class, () -> original.getOrders().clear());
        expect(UnsupportedOperationException.class, () -> restarted.getReceipts().clear());
    }

    private static void roundTripAndExport(Path directory) throws Exception {
        FileReceiptRepository repository = new FileReceiptRepository(directory.resolve("receipts"));
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        history.refresh();
        Receipt first = receipt("2026-10-10T01:00:00Z", List.of(order("Rice", 1, "5.00")));
        Receipt second = receipt("2026-10-11T02:00:00Z", List.of(order("Fish Fillet", 2, "12.00")));
        history.save(first);
        history.save(second);
        Path output = directory.resolve("exported receipt.txt");
        ReceiptService formatter = new ReceiptService();
        formatter.export(second, output);
        history.markExported(second, output);
        history.refresh();
        truth(history.getReceipts().size() == 2, "saved and exported receipts reload");
        Receipt loaded = history.getReceipts().get(0);
        truth(loaded.getId().equals(second.getId()), "history loads newest receipts first");
        truth(loaded.isExported() && loaded.getExportPath().equals(output.toAbsolutePath().toString()), "export metadata persists");
        truth(formatter.format(loaded).equals(Files.readString(output)), "reopened receipt matches exported text");
        truth(formatter.format(loaded).contains(loaded.getNumber()), "export includes receipt number");
    }

    private static void noDuplicateOrDowngrade(Path directory) throws Exception {
        ReceiptHistoryService history = new ReceiptHistoryService(new FileReceiptRepository(directory));
        history.refresh();
        Receipt receipt = Receipt.capture(List.of(order("Rice", 1, "5.00")));
        history.save(receipt);
        history.save(receipt);
        history.markExported(receipt, directory.resolveSibling("receipt.txt"));
        history.save(receipt);
        truth(history.getReceipts().size() == 1, "saving or exporting a receipt never duplicates its ID");
        truth(history.getReceipts().get(0).isExported(), "saving a stale modal snapshot cannot remove export status");
        Receipt changed = new Receipt(receipt.getId(), receipt.getCreatedAt(), List.of(order("Rice", 9, "10.00")), "");
        expect(IOException.class, () -> history.save(changed));
        money("5.00", history.getReceipts().get(0).getTotal(), "an existing receipt cannot be rewritten with different orders");
    }

    private static void failedWrites() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        Receipt first = Receipt.capture(List.of(order("Rice", 1, "5.00")));
        Receipt second = Receipt.capture(List.of(order("Soup", 1, "3.00")));
        expect(IOException.class, () -> history.save(first));
        history.refresh();
        history.save(first);
        repository.fail = true;
        expect(IOException.class, () -> history.save(second));
        expect(IOException.class, () -> history.markExported(first, Path.of("receipt.txt")));
        truth(history.getReceipts().size() == 1 && !history.getReceipts().get(0).isExported(),
                "failed saves preserve the previous receipt snapshot and status");
    }

    private static void malformedHistory(Path directory) throws Exception {
        FileReceiptRepository repository = new FileReceiptRepository(directory);
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        history.refresh();
        Receipt saved = Receipt.capture(List.of(order("Rice", 1, "5.00")));
        history.save(saved);
        Path broken = directory.resolve(saved.getId() + ".receipt");
        String badContents = "version=1\nid=" + saved.getId() + "\ncreatedAt=bad-date\norderCount=1\n";
        Files.writeString(broken, badContents);
        expect(IOException.class, history::refresh);
        expect(IOException.class, () -> history.save(Receipt.capture(List.of(order("Soup", 1, "3.00")))));
        expect(IOException.class, () -> history.deleteSelected(List.of(saved.getId())));
        expect(IOException.class, history::clear);
        truth(history.getReceipts().size() == 1, "failed loading keeps the previous readable snapshot");
        truth(Files.readString(broken).equals(badContents), "malformed history is never overwritten");
        repository.save(saved);
        history.refresh();
        history.save(Receipt.capture(List.of(order("Soup", 1, "3.00"))));
        truth(history.getReceipts().size() == 2, "saving resumes after successful repair and reload");
    }

    private static void failedExport(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path target = directory.resolve("cannot-replace.txt");
        Files.createDirectory(target);
        Path existing = target.resolve("existing.txt");
        Files.writeString(existing, "existing contents");
        Receipt receipt = Receipt.capture(List.of(order("Rice", 1, "5.00")));
        expect(IOException.class, () -> new ReceiptService().export(receipt, target));
        truth(Files.readString(existing).equals("existing contents"), "failed export preserves the destination");
        try (Stream<Path> files = Files.list(directory)) {
            truth(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")), "failed export cleans temporary files");
        }
    }

    private static void deleteSelectedAndRestart(Path directory) throws Exception {
        FileReceiptRepository repository = new FileReceiptRepository(directory.resolve("receipts"));
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        history.refresh();
        Receipt first = receipt("2026-10-10T01:00:00Z", List.of(order("Rice", 1, "5")));
        Receipt second = receipt("2026-10-11T02:00:00Z", List.of(order("Fish", 2, "12")));
        Receipt third = receipt("2026-10-12T03:00:00Z", List.of(order("Soup", 1, "3")));
        history.save(first);
        history.save(second);
        history.save(third);
        Path exported = directory.resolve("exported.txt");
        new ReceiptService().export(second, exported);
        history.markExported(second, exported);
        String exportedText = Files.readString(exported);
        truth(history.deleteSelected(List.of(second.getId(), second.getId())) == 1,
                "duplicate selected IDs delete one receipt");
        truth(Files.notExists(directory.resolve("receipts").resolve(second.getId() + ".receipt")),
                "deletion removes the selected archive file");
        truth(Files.readString(exported).equals(exportedText), "deletion keeps the exported text file");
        ReceiptHistoryService restarted = new ReceiptHistoryService(repository);
        restarted.refresh();
        truth(restarted.getReceipts().stream().map(Receipt::getId).toList().equals(List.of(third.getId(), first.getId())),
                "deletion survives restart and keeps unselected receipts");
        truth(restarted.deleteSelected(List.of()) == 0, "empty selection deletes nothing");
        expect(IOException.class, () -> restarted.deleteSelected(List.of(first.getId(), UUID.randomUUID())));
        truth(repository.load().size() == 2, "a stale selection is rejected before any deletion");
    }

    private static void clearCache(Path directory) throws Exception {
        Path archive = directory.resolve("receipts");
        ReceiptHistoryService history = new ReceiptHistoryService(new FileReceiptRepository(archive));
        history.refresh();
        Receipt first = Receipt.capture(List.of(order("Rice", 1, "5")));
        history.save(first);
        history.save(Receipt.capture(List.of(order("Soup", 1, "3"))));
        Path exported = directory.resolve("exported.txt");
        new ReceiptService().export(first, exported);
        history.markExported(first, exported);
        String exportedText = Files.readString(exported);
        Path ordersFile = directory.resolve("orders.txt");
        FileOrderRepository orders = new FileOrderRepository(ordersFile);
        List<Order> currentOrders = List.of(order("Fish", 2, "12"));
        orders.save(currentOrders);
        Path note = archive.resolve("keep.txt");
        Files.writeString(note, "Keep this file");
        Path nested = archive.resolve("other-files").resolve("keep.txt");
        Files.createDirectories(nested.getParent());
        Files.writeString(nested, "Keep this nested file");

        truth(history.clear() == 2, "clear removes all saved receipts");
        history.refresh();
        truth(history.getReceipts().isEmpty(), "cleared history stays empty after reloading");
        truth(history.clear() == 0, "clearing an empty history is harmless");
        truth(orders.load().equals(currentOrders), "clearing receipts keeps current orders");
        truth(Files.readString(exported).equals(exportedText), "clearing receipts keeps exported text");
        truth(Files.readString(note).equals("Keep this file") && Files.readString(nested).equals("Keep this nested file"),
                "clear removes only receipt archives, without recursively deleting other files");
    }

    private static void failedDeletes() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        Receipt first = Receipt.capture(List.of(order("Rice", 1, "5")));
        Receipt second = Receipt.capture(List.of(order("Soup", 1, "3")));
        expect(IOException.class, () -> history.deleteSelected(List.of(first.getId())));
        expect(IOException.class, history::clear);
        history.refresh();
        history.save(first);
        history.save(second);
        repository.failDelete = first.getId();
        expect(IOException.class, () -> history.deleteSelected(List.of(first.getId())));
        truth(history.getReceipts().size() == 2 && repository.load().size() == 2,
                "failed deletion preserves the snapshot and archive");
        expect(IOException.class, history::clear);
        expect(IOException.class, () -> history.save(Receipt.capture(List.of(order("Fish", 1, "8")))));
        history.refresh();
        repository.failDelete = second.getId();
        expect(IOException.class, () -> history.deleteSelected(List.of(first.getId(), second.getId())));
        truth(history.getReceipts().size() == 1 && history.getReceipts().get(0).getId().equals(second.getId()),
                "a partial failure publishes completed deletions and keeps the failed receipt");
        truth(repository.load().size() == 1, "partial deletion snapshot matches storage");
        history.refresh();
        repository.failDelete = null;
        truth(history.clear() == 1 && repository.load().isEmpty(), "deletion resumes after a successful reload");
    }

    private static void protectedDeletionTarget(Path directory) throws Exception {
        FileReceiptRepository repository = new FileReceiptRepository(directory);
        ReceiptHistoryService history = new ReceiptHistoryService(repository);
        history.refresh();
        Receipt receipt = Receipt.capture(List.of(order("Rice", 1, "5")));
        history.save(receipt);
        Path file = directory.resolve(receipt.getId() + ".receipt");
        Files.delete(file);
        Files.createDirectory(file);
        Path unrelated = file.resolve("keep.txt");
        Files.writeString(unrelated, "Keep this file");
        expect(IOException.class, () -> history.deleteSelected(List.of(receipt.getId())));
        truth(Files.readString(unrelated).equals("Keep this file"), "deletion never removes a replaced directory");
        truth(history.getReceipts().size() == 1, "a replaced archive remains visible until a successful reload");
        Files.delete(unrelated);
        Files.delete(file);
        history.refresh();
        truth(history.getReceipts().isEmpty(), "refresh reconciles externally removed archives");
        repository.delete(receipt.getId());
    }

    private static Order order(String dish, int quantity, String price) {
        return new Order(dish, quantity, new BigDecimal(price), BigDecimal.ZERO);
    }
    private static Receipt receipt(String instant, List<Order> orders) {
        return new Receipt(UUID.randomUUID(), Instant.parse(instant), orders, "");
    }
    private static void money(String expected, BigDecimal actual, String message) {
        truth(new BigDecimal(expected).compareTo(actual) == 0, message);
    }
    private static void truth(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static void expect(Class<? extends Throwable> type, Action action) throws Exception {
        try { action.run(); }
        catch (Throwable exception) {
            if (type.isInstance(exception)) { return; }
            throw new AssertionError("Expected " + type.getSimpleName(), exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static final class MemoryRepository implements ReceiptRepository {
        private final List<Receipt> receipts = new ArrayList<>();
        private boolean fail;
        private UUID failDelete;
        @Override public List<Receipt> load() { return List.copyOf(receipts); }
        @Override public void save(Receipt receipt) throws IOException {
            if (fail) { throw new IOException("Simulated write failure"); }
            receipts.removeIf(saved -> saved.getId().equals(receipt.getId()));
            receipts.add(receipt);
        }
        @Override public void delete(UUID id) throws IOException {
            if (id.equals(failDelete)) { throw new IOException("Simulated deletion failure"); }
            receipts.removeIf(saved -> saved.getId().equals(id));
        }
    }
}
