package finalPPKG.ui.components;

import finalPPKG.model.MenuItem;
import finalPPKG.ui.theme.Theme;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;

/** Classpath-ready PNG copies of the supplied ui/components/icons ICO artwork. */
public final class MenuIcons {
    private static final Map<String, String> DISH_FILES = Map.ofEntries(
            Map.entry("Fried Chicken /w rice", "fried_chicken"),
            Map.entry("Pork Steak", "pork_steak"),
            Map.entry("Fish Fillet", "fish_fillet"),
            Map.entry("Sinabawang Isda Barilison", "sabaw-isda_barilison"),
            Map.entry("Rice", "rice"),
            Map.entry("Coca-cola (Litro)", "coca-cola_litro"),
            Map.entry("Gitaang Langka", "ginataang-langka"),
            Map.entry("Afritada Pork", "afritda_pork"),
            Map.entry("Royal Tru Orange (Litre Plastic)", "royal-tru_orange-plastic"));
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

    private MenuIcons() { }

    public static Icon dish(MenuItem item, int size) {
        String file = DISH_FILES.get(item.getName());
        AppIcon.Kind fallback = item.getCategory().equals("Drinks") ? AppIcon.Kind.DRINK : AppIcon.Kind.TRAY;
        return file == null ? new AppIcon(fallback, size, Theme.PRIMARY)
                : load("order_details/" + file, size, fallback);
    }

    public static Icon currentOrder(int size) { return load("current_order", size, AppIcon.Kind.ORDERS); }
    public static Icon orderTotal(int size) { return load("order_total", size, AppIcon.Kind.RECEIPT); }

    private static Icon load(String name, int size, AppIcon.Kind fallback) {
        if (size < 1) { throw new IllegalArgumentException("Icon size must be positive."); }
        BufferedImage image;
        synchronized (IMAGES) {
            if (!IMAGES.containsKey(name)) {
                try (InputStream input = MenuIcons.class.getResourceAsStream("/icons/" + name + ".png")) {
                    IMAGES.put(name, input == null ? null : ImageIO.read(input));
                } catch (IOException exception) {
                    IMAGES.put(name, null);
                }
            }
            image = IMAGES.get(name);
        }
        return image == null ? new AppIcon(fallback, size, Theme.PRIMARY) : new BitmapIcon(image, size);
    }

    private record BitmapIcon(BufferedImage image, int size) implements Icon {
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.drawImage(image, x, y, size, size, null);
            canvas.dispose();
        }
    }
}
