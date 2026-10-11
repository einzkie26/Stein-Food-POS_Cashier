package finalPPKG.repository;

import finalPPKG.model.Receipt;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Receipt history storage can be replaced independently of the Swing screens. */
public interface ReceiptRepository {
    List<Receipt> load() throws IOException;
    void save(Receipt receipt) throws IOException;
    void delete(UUID id) throws IOException;
}
