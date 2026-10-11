package finalPPKG.model;

/** A menu label and its grouping. Prices remain an order-time choice. */
public final class MenuItem {
    private final String name;
    private final String category;

    public MenuItem(String name, String category) {
        if (name == null || name.isBlank() || category == null || category.isBlank()) {
            throw new IllegalArgumentException("Menu name and category are required.");
        }
        if (name.indexOf(':') >= 0 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Menu names cannot contain colons or line breaks.");
        }
        this.name = name.strip();
        this.category = category.strip();
    }

    public String getName() { return name; }
    public String getCategory() { return category; }

    @Override
    public String toString() { return name; }
}
