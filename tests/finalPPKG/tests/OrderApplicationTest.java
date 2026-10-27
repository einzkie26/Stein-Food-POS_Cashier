package finalPPKG.tests;

import finalPPKG.app.AppConfig;
import finalPPKG.model.MenuItem;
import finalPPKG.model.Order;
import finalPPKG.repository.FileOrderRepository;
import finalPPKG.repository.OrderRepository;
import finalPPKG.service.MenuCatalog;
import finalPPKG.service.OrderService;
import finalPPKG.service.ReceiptService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Dependency-free behavior checks; run with .\build.ps1 -Test. */
public final class OrderApplicationTest {
    private static int passed;

    private OrderApplicationTest() { }

    public static void main(String[] args) throws Exception {
        Path temporaryDirectory = Files.createTempDirectory("stein-order-tests-").toAbsolutePath().normalize();
        try {
            check("money calculations and rounding", OrderApplicationTest::moneyCalculations);
            check("order constraints", OrderApplicationTest::orderConstraints);
            check("installed data location and explicit overrides", OrderApplicationTest::dataDirectories);
            check("service mutations and immutable snapshots", OrderApplicationTest::serviceMutations);
            check("failed writes preserve in-memory orders", OrderApplicationTest::failedWrites);
            check("writes require successfully loaded history", OrderApplicationTest::loadBeforeWrites);
            check("legacy persistence and round-trip", () -> persistenceRoundTrip(temporaryDirectory));
            check("malformed data remains intact", () -> malformedData(temporaryDirectory));
            check("menu asset loads from the classpath", OrderApplicationTest::menuAsset);
            check("receipt totals and export", () -> receipt(temporaryDirectory));
            System.out.println("All " + passed + " behavior checks passed.");
        } finally {
            deleteTemporaryDirectory(temporaryDirectory);
        }
    }

    private static void moneyCalculations() {
        Order discounted = order("Pork Adobo", 3, "19.99", "12.5");
        money("59.97", discounted.getSubtotal(), "subtotal");
        money("7.50", discounted.getDiscountAmount(), "discount rounded to cents");
        money("52.47", discounted.getTotal(), "total");
        Order halfCent = order("Rice", 1, "0.05", "10");
        money("0.01", halfCent.getDiscountAmount(), "half-cent discount rounds up");
        money("0.04", halfCent.getTotal(), "rounded discount deducted once");
        Order fractionalPrice = order("Rice", 2, "2.345", "0");
        money("2.35", fractionalPrice.getUnitPrice(), "unit price rounds before multiplication");
        money("4.70", fractionalPrice.getSubtotal(), "rounded unit price determines subtotal");
        money("0.00", order("Rice", 1, "4.50", "100").getTotal(), "full discount");
    }

    private static void dataDirectories() {
        String previousDirectory = System.getProperty("stein.data.dir");
        String previousPackaged = System.getProperty("stein.packaged");
        try {
            System.clearProperty("stein.data.dir");
            System.clearProperty("stein.packaged");
            equal(Path.of(".").toAbsolutePath().normalize(), new AppConfig().getDataDirectory(),
                    "development launch retains working-directory storage");
            System.setProperty("stein.packaged", "true");
            String localAppData = System.getenv("LOCALAPPDATA");
            Path profile = localAppData == null || localAppData.isBlank()
                    ? Path.of(System.getProperty("user.home"), "AppData", "Local") : Path.of(localAppData);
            Path expected = profile.resolve("SteinFoodHub").toAbsolutePath().normalize();
            equal(expected, new AppConfig().getDataDirectory(), "installed data is independent of the program directory");
            equal(expected.resolve("order_history.txt"), new AppConfig().getOrderFile(), "installed order history path");
            equal(expected.resolve("receipt-history"), new AppConfig().getReceiptHistoryDirectory(), "installed receipt archive path");
            Path override = Path.of("build", "custom-data").toAbsolutePath().normalize();
            System.setProperty("stein.data.dir", override.toString());
            equal(override, new AppConfig().getDataDirectory(), "explicit data directory wins over packaged defaults");
        } finally {
            restoreProperty("stein.data.dir", previousDirectory);
            restoreProperty("stein.packaged", previousPackaged);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) { System.clearProperty(name); }
        else { System.setProperty(name, value); }
    }

    private static void orderConstraints() throws Exception {
        expect(IllegalArgumentException.class, () -> order("Rice", 0, "2.00", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice", -1, "2.00", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice", 10_000, "2.00", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice", 1, "-0.01", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice", 1, "1000000000", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice", 1, "2.00", "-0.01"));
        expect(IllegalArgumentException.class, () -> order("Rice", 1, "2.00", "100.01"));
        expect(IllegalArgumentException.class, () -> order(" ", 1, "2.00", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice\nSoup", 1, "2.00", "0"));
        expect(IllegalArgumentException.class, () -> order("Rice : Soup", 1, "2.00", "0"));
    }

    private static void serviceMutations() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        OrderService service = new OrderService(repository);
        service.refresh();
        Order first = order("Rice", 2, "5.00", "0");
        Order duplicateDish = order("Rice", 1, "7.00", "0");
        Order third = order("Soup", 3, "4.00", "25");
        service.add(first);
        service.add(duplicateDish);
        service.add(third);
        money("26.00", service.total(), "aggregate discounted total");
        equal(6, service.itemCount(), "item count includes quantities");
        List<Order> snapshot = service.getOrders();
        expect(UnsupportedOperationException.class, () -> snapshot.add(first));

        Order replacement = order("Rice", 4, "3.00", "0");
        service.update(1, replacement);
        sameOrder(first, service.getOrders().get(0));
        sameOrder(replacement, service.getOrders().get(1));
        sameOrder(third, service.getOrders().get(2));
        equal(3, snapshot.size(), "previous snapshot retains its rows");
        sameOrder(duplicateDish, snapshot.get(1));
        service.remove(1);
        equal(2, service.getOrders().size(), "remove selected row");
        sameOrder(third, service.getOrders().get(1));
        money("19.00", service.total(), "remaining total");
        int writesBeforeInvalidIndex = repository.writes;
        expect(IndexOutOfBoundsException.class, () -> service.update(2, replacement));
        expect(IndexOutOfBoundsException.class, () -> service.remove(-1));
        equal(writesBeforeInvalidIndex, repository.writes, "invalid selection does not save");
        service.clear();
        equal(0, service.getOrders().size(), "clear all rows");
        equal(0, repository.orders.size(), "clear persists empty state");
        money("0.00", service.total(), "empty total");
    }

    private static void failedWrites() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        Order first = order("Rice", 2, "5.00", "0");
        repository.orders.add(first);
        OrderService service = new OrderService(repository);
        service.refresh();
        repository.failSave = true;
        expect(IOException.class, () -> service.add(order("Soup", 1, "3.00", "0")));
        expect(IOException.class, () -> service.update(0, order("Rice", 1, "1.00", "0")));
        expect(IOException.class, () -> service.remove(0));
        expect(IOException.class, service::clear);
        equal(1, service.getOrders().size(), "failed mutations preserve row count");
        sameOrder(first, service.getOrders().get(0));
        sameOrder(first, repository.orders.get(0));
        repository.failLoad = true;
        expect(IOException.class, service::refresh);
        sameOrder(first, service.getOrders().get(0));
    }

    private static void loadBeforeWrites() throws Exception {
        MemoryRepository repository = new MemoryRepository();
        Order saved = order("Rice", 2, "5.00", "0");
        Order added = order("Soup", 1, "3.00", "0");
        repository.orders.add(saved);
        OrderService service = new OrderService(repository);
        expect(IOException.class, () -> service.add(added));
        expect(IOException.class, service::clear);
        equal(0, repository.writes, "unloaded history cannot be overwritten");
        sameOrder(saved, repository.orders.get(0));

        service.refresh();
        repository.failLoad = true;
        expect(IOException.class, service::refresh);
        expect(IOException.class, () -> service.add(added));
        expect(IOException.class, () -> service.update(0, added));
        expect(IOException.class, () -> service.remove(0));
        expect(IOException.class, service::clear);
        equal(0, repository.writes, "a failed refresh locks all writes");
        sameOrder(saved, service.getOrders().get(0));
        sameOrder(saved, repository.orders.get(0));

        repository.failLoad = false;
        service.refresh();
        service.add(added);
        equal(1, repository.writes, "a successful reload unlocks saving");
        equal(2, service.getOrders().size(), "recovered history keeps previous orders");
    }

    private static void persistenceRoundTrip(Path directory) throws Exception {
        Path history = directory.resolve("legacy/order_history.txt");
        FileOrderRepository repository = new FileOrderRepository(history);
        equal(0, repository.load().size(), "missing history is empty");
        Files.createDirectories(history.getParent());
        Files.writeString(history,
                "Pork Adobo : 3 : 19.99 : 12.5 : 52.47375\nRice:2:5.0:0.0:10.0\n",
                StandardCharsets.UTF_8);
        List<Order> loaded = repository.load();
        equal(2, loaded.size(), "legacy rows loaded");
        sameOrder(order("Pork Adobo", 3, "19.99", "12.5"), loaded.get(0));
        sameOrder(order("Rice", 2, "5.00", "0"), loaded.get(1));
        repository.save(loaded);
        List<Order> reloaded = new FileOrderRepository(history).load();
        equal(loaded.size(), reloaded.size(), "round-trip row count");
        for (int index = 0; index < loaded.size(); index++) {
            sameOrder(loaded.get(index), reloaded.get(index));
        }
        Files.writeString(history, "Rice:1:0.01:99.999999:9.999999939225291E-11\n",
                StandardCharsets.UTF_8);
        List<Order> tinyLegacyTotal = repository.load();
        equal(1, tinyLegacyTotal.size(), "legacy floating-point exponent total loads");
        money("0.00", tinyLegacyTotal.get(0).getTotal(), "legacy tiny total rounds to cents");
        repository.save(List.of());
        equal(0, repository.load().size(), "empty history round-trip");
    }

    private static void malformedData(Path directory) throws Exception {
        Path history = directory.resolve("malformed.txt");
        FileOrderRepository repository = new FileOrderRepository(history);
        Order first = order("Rice", 1, "5.00", "0");
        repository.save(List.of(first));
        OrderService service = new OrderService(repository);
        service.refresh();
        String[] invalidRows = {
                "Rice:1:5:0\n",
                "Rice:0:5:0:0\n",
                "Rice:1:NaN:0:0\n",
                "Rice:1:5:101:0\n",
                "Rice:1:5:0:garbage\n"
        };
        for (String invalidRow : invalidRows) {
            String malformed = "Rice:1:5:0:5\n" + invalidRow;
            Files.writeString(history, malformed, StandardCharsets.UTF_8);
            expect(IOException.class, repository::load);
            expect(IOException.class, service::refresh);
            equal(malformed, Files.readString(history, StandardCharsets.UTF_8), "bad file not rewritten");
            equal(1, service.getOrders().size(), "partial data not applied");
            sameOrder(first, service.getOrders().get(0));
        }
    }

    private static void menuAsset() throws Exception {
        MenuCatalog catalog = new MenuCatalog();
        truth(!catalog.getItems().isEmpty(), "menu resource contains dishes");
        for (MenuItem item : catalog.getItems()) {
            truth(!item.getName().isBlank(), "dish has a readable name");
            truth(!item.getCategory().isBlank(), "dish has a category");
        }
    }

    private static void receipt(Path directory) throws Exception {
        List<Order> orders = List.of(order("Pork Adobo", 3, "19.99", "12.5"),
                order("Rice", 2, "5.00", "0"));
        ReceiptService receipts = new ReceiptService();
        String text = receipts.format(orders);
        truth(text.contains("Pork Adobo") && text.contains("Rice"), "receipt contains every dish");
        truth(text.contains("69.97"), "receipt includes subtotal");
        truth(text.contains("7.50"), "receipt includes total discount");
        truth(text.contains("62.47"), "receipt includes amount due");
        Path output = directory.resolve("receipt.txt");
        receipts.export(orders, output);
        equal(text, Files.readString(output, StandardCharsets.UTF_8), "export matches preview");
        truth(receipts.format(List.of()).contains("0.00"), "empty receipt has zero total");
    }

    private static Order order(String dish, int quantity, String unitPrice, String discount) {
        return new Order(dish, quantity, new BigDecimal(unitPrice), new BigDecimal(discount));
    }

    private static void sameOrder(Order expected, Order actual) {
        equal(expected.getDish(), actual.getDish(), "dish");
        equal(expected.getQuantity(), actual.getQuantity(), "quantity");
        money(expected.getUnitPrice().toPlainString(), actual.getUnitPrice(), "unit price");
        money(expected.getDiscountPercent().toPlainString(), actual.getDiscountPercent(), "discount percent");
        money(expected.getTotal().toPlainString(), actual.getTotal(), "total");
    }

    private static void money(String expected, BigDecimal actual, String message) {
        if (new BigDecimal(expected).compareTo(actual) != 0) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void truth(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }

    private static void expect(Class<? extends Throwable> expected, CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (Throwable error) {
            if (expected.isInstance(error)) { return; }
            throw new AssertionError("Expected " + expected.getSimpleName() + ", got " + error, error);
        }
        throw new AssertionError("Expected " + expected.getSimpleName() + " to be thrown.");
    }

    private static void check(String name, CheckedAction action) throws Exception {
        action.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void deleteTemporaryDirectory(Path directory) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Path candidate = path.toAbsolutePath().normalize();
                if (!candidate.startsWith(root)) {
                    throw new IOException("Refusing to delete a path outside the test directory: " + candidate);
                }
                Files.delete(candidate);
            }
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static final class MemoryRepository implements OrderRepository {
        private List<Order> orders = new ArrayList<>();
        private boolean failSave;
        private boolean failLoad;
        private int writes;

        @Override
        public List<Order> load() throws IOException {
            if (failLoad) { throw new IOException("Simulated read failure"); }
            return new ArrayList<>(orders);
        }

        @Override
        public void save(List<Order> replacement) throws IOException {
            if (failSave) { throw new IOException("Simulated write failure"); }
            writes++;
            orders = new ArrayList<>(replacement);
        }
    }
}
