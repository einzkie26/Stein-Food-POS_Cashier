package finalPPKG.service;

import finalPPKG.model.MenuItem;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Loads the editable resources/menu.csv asset from the application classpath. */
public final class MenuCatalog {
    private final List<MenuItem> items;

    public MenuCatalog() throws IOException {
        InputStream stream = MenuCatalog.class.getResourceAsStream("/menu.csv");
        if (stream == null) {
            throw new IOException("Menu asset /menu.csv is missing. Build the app with its resources folder.");
        }
        List<MenuItem> loaded = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header != null && header.startsWith("\uFEFF")) header = header.substring(1);
            if (!"category,name".equals(header)) {
                throw new IOException("menu.csv must start with the header category,name.");
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;
                List<String> fields = parseCsvLine(line, lineNumber);
                if (fields.size() != 2) throw invalidLine(lineNumber, "Expected category and name.");
                try {
                    loaded.add(new MenuItem(fields.get(1), fields.get(0)));
                } catch (IllegalArgumentException ex) {
                    throw invalidLine(lineNumber, ex.getMessage());
                }
            }
        }
        if (loaded.isEmpty()) throw new IOException("menu.csv must contain at least one menu item.");
        items = List.copyOf(loaded);
    }

    public List<MenuItem> getItems() { return items; }

    // Supports quoted commas and escaped quotes, keeping the two-column asset easy to edit.
    private static List<String> parseCsvLine(String line, int lineNumber) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (quoted) {
                if (character == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(character);
                }
            } else if (character == ',') {
                fields.add(field.toString().strip());
                field.setLength(0);
                quoteClosed = false;
            } else if (character == '"' && field.toString().isBlank() && !quoteClosed) {
                field.setLength(0);
                quoted = true;
            } else if (character == '"' || (quoteClosed && !Character.isWhitespace(character))) {
                throw invalidLine(lineNumber, "Invalid CSV quoting.");
            } else {
                field.append(character);
            }
        }
        if (quoted) throw invalidLine(lineNumber, "A quoted field is not closed.");
        fields.add(field.toString().strip());
        return fields;
    }

    private static IOException invalidLine(int lineNumber, String reason) {
        return new IOException("Cannot read menu.csv at line " + lineNumber + ": " + reason);
    }
}
