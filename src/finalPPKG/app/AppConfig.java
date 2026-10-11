package finalPPKG.app;

import java.nio.file.Path;
import java.nio.file.Paths;

/** All writable application files are located together; resources stay read-only. */
public final class AppConfig {
    private final Path dataDirectory;

    public AppConfig() {
        this(Paths.get(System.getProperty("stein.data.dir", ".")));
    }

    public AppConfig(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    public Path getDataDirectory() { return dataDirectory; }
    public Path getOrderFile() { return dataDirectory.resolve("order_history.txt"); }
    public Path getReceiptFile() { return dataDirectory.resolve("receipt.txt"); }
    public Path getReceiptHistoryDirectory() { return dataDirectory.resolve("receipt-history"); }
}
