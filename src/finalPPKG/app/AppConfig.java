package finalPPKG.app;

import java.nio.file.Path;
import java.nio.file.Paths;

/** All writable application files are located together; resources stay read-only. */
public final class AppConfig {
    private final Path dataDirectory;

    public AppConfig() {
        this(defaultDataDirectory());
    }

    public AppConfig(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    private static Path defaultDataDirectory() {
        String override = System.getProperty("stein.data.dir");
        if (override != null && !override.isBlank()) { return Paths.get(override); }
        if (Boolean.getBoolean("stein.packaged")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path root = localAppData == null || localAppData.isBlank()
                    ? Paths.get(System.getProperty("user.home"), "AppData", "Local") : Paths.get(localAppData);
            return root.resolve("SteinFoodHub");
        }
        return Paths.get(".");
    }

    public Path getDataDirectory() { return dataDirectory; }
    public Path getOrderFile() { return dataDirectory.resolve("order_history.txt"); }
    public Path getReceiptFile() { return dataDirectory.resolve("receipt.txt"); }
    public Path getReceiptHistoryDirectory() { return dataDirectory.resolve("receipt-history"); }
}
