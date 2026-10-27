package finalPPKG.ui.components;

import finalPPKG.ui.theme.Theme;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Fits the supplied border artwork to a card while leaving its center clear. */
final class SelectionBorder {
    static final int FRAME_COUNT = 4;
    private final BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
    private final Rectangle artwork = new Rectangle();

    SelectionBorder() {
        for (int frame = 0; frame < frames.length; frame++) {
            BufferedImage image = MenuIcons.image("order_details/cake_border/selection_border" + (frame + 1));
            frames[frame] = image;
            if (image != null) {
                Rectangle bounds = visibleBounds(image);
                if (artwork.isEmpty()) { artwork.setBounds(bounds); }
                else { artwork.add(bounds); }
            }
        }
    }

    void paint(Graphics2D canvas, Component component, int frame, int x, int y, int width, int height) {
        BufferedImage image = frames[frame];
        if (image == null || artwork.isEmpty()) {
            canvas.setColor(Theme.PRIMARY);
            canvas.setStroke(new BasicStroke(2));
            canvas.drawRoundRect(x + 1, y + 1, width - 2, height - 2, 12, 12);
            return;
        }
        Graphics2D border = (Graphics2D) canvas.create();
        border.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int edge = Math.min(8, Math.min(width, height) / 3);
        int[] destinationX = {x, x + edge, x + width - edge, x + width};
        int[] destinationY = {y, y + edge, y + height - edge, y + height};
        // Broad source slices include the bent edges of every frame. Their inner transparency is preserved.
        int[] sourceX = {artwork.x, artwork.x + artwork.width / 4,
                artwork.x + artwork.width * 3 / 4, artwork.x + artwork.width};
        int[] sourceY = {artwork.y, artwork.y + artwork.height * 3 / 10,
                artwork.y + artwork.height * 4 / 5, artwork.y + artwork.height};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (row == 1 && column == 1) { continue; }
                border.drawImage(image, destinationX[column], destinationY[row],
                        destinationX[column + 1], destinationY[row + 1],
                        sourceX[column], sourceY[row], sourceX[column + 1], sourceY[row + 1], component);
            }
        }
        border.dispose();
    }

    private static Rectangle visibleBounds(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((pixels[y * width + x] >>> 24) >= 32) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return maxX < 0 ? new Rectangle() : new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
