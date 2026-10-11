package finalPPKG.service;

import finalPPKG.model.Receipt;
import finalPPKG.repository.ReceiptRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Keeps saved/exported receipts without duplicating a receipt when it is re-exported. */
public final class ReceiptHistoryService {
    private final ReceiptRepository repository;
    private List<Receipt> receipts = List.of();
    private boolean loaded;

    public ReceiptHistoryService(ReceiptRepository repository) { this.repository = repository; }
    public List<Receipt> getReceipts() { return receipts; }

    public void refresh() throws IOException {
        loaded = false;
        receipts = List.copyOf(repository.load());
        loaded = true;
    }

    public Receipt save(Receipt receipt) throws IOException {
        requireLoaded();
        Receipt existing = receipts.stream().filter(saved -> saved.getId().equals(receipt.getId())).findFirst().orElse(null);
        if (existing != null) {
            if (!existing.getCreatedAt().equals(receipt.getCreatedAt()) || !existing.getOrders().equals(receipt.getOrders())) {
                throw new IOException("An existing receipt cannot be replaced with different orders.");
            }
            return existing;
        }
        persist(receipt);
        return receipt;
    }

    public Receipt markExported(Receipt receipt, Path destination) throws IOException {
        Receipt updated = save(receipt).withExportPath(destination);
        persist(updated);
        return updated;
    }

    public int deleteSelected(List<UUID> ids) throws IOException {
        requireLoaded();
        List<UUID> selected = List.copyOf(ids).stream().distinct().toList();
        Set<UUID> savedIds = receipts.stream().map(Receipt::getId).collect(Collectors.toSet());
        if (!savedIds.containsAll(selected)) {
            throw new IOException("A selected receipt is no longer in history. Refresh and select it again.");
        }
        int removed = 0;
        for (UUID id : selected) {
            try {
                repository.delete(id);
            } catch (IOException exception) {
                loaded = false;
                throw new IOException("Deleted " + removed + " of " + selected.size()
                        + " receipts. " + exception.getMessage() + " Refresh receipt history before trying again.", exception);
            }
            // Publish each completed deletion so a later failure cannot leave stale rows.
            receipts = receipts.stream().filter(receipt -> !receipt.getId().equals(id)).toList();
            removed++;
        }
        return removed;
    }

    public int clear() throws IOException {
        return deleteSelected(receipts.stream().map(Receipt::getId).toList());
    }

    private void persist(Receipt receipt) throws IOException {
        repository.save(receipt);
        List<Receipt> updated = new ArrayList<>(receipts);
        updated.removeIf(saved -> saved.getId().equals(receipt.getId()));
        updated.add(receipt);
        updated.sort(Comparator.comparing(Receipt::getCreatedAt).reversed());
        receipts = List.copyOf(updated);
    }

    private void requireLoaded() throws IOException {
        if (!loaded) { throw new IOException("Reload receipt history successfully before changing saved receipts."); }
    }
}
