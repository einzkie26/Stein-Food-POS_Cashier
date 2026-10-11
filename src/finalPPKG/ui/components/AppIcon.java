package finalPPKG.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Small vector icons remain sharp at any display scale. */
public final class AppIcon implements Icon {
    public enum Kind { PLUS, ORDERS, RECEIPT, SETTINGS, LEAF }

    private final Kind kind;
    private final int size;
    private final Color color;

    public AppIcon(Kind kind, int size, Color color) {
        if (size < 1) {
            throw new IllegalArgumentException("Icon size must be positive.");
        }
        this.kind = java.util.Objects.requireNonNull(kind, "kind");
        this.size = size;
        this.color = java.util.Objects.requireNonNull(color, "color");
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.translate(x, y);
        canvas.scale(size / 24.0, size / 24.0);
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.setColor(color);
        switch (kind) {
            case PLUS:
                line(canvas, 12, 5, 12, 19);
                line(canvas, 5, 12, 19, 12);
                break;
            case ORDERS:
                canvas.draw(new RoundRectangle2D.Double(4, 3, 16, 18, 4, 4));
                line(canvas, 8, 8, 16, 8);
                line(canvas, 8, 12, 16, 12);
                line(canvas, 8, 16, 13, 16);
                break;
            case RECEIPT:
                Path2D receipt = new Path2D.Double();
                receipt.moveTo(5, 3);
                receipt.lineTo(19, 3);
                receipt.lineTo(19, 21);
                receipt.lineTo(15.5, 18.5);
                receipt.lineTo(12, 21);
                receipt.lineTo(8.5, 18.5);
                receipt.lineTo(5, 21);
                receipt.closePath();
                canvas.draw(receipt);
                line(canvas, 8, 8, 16, 8);
                line(canvas, 8, 12, 16, 12);
                break;
            case SETTINGS:
                canvas.draw(new Ellipse2D.Double(5, 5, 14, 14));
                canvas.draw(new Ellipse2D.Double(9, 9, 6, 6));
                for (int index = 0; index < 8; index++) {
                    double angle = index * Math.PI / 4;
                    line(canvas, 12 + Math.cos(angle) * 7, 12 + Math.sin(angle) * 7,
                            12 + Math.cos(angle) * 9.5, 12 + Math.sin(angle) * 9.5);
                }
                break;
            case LEAF:
                Path2D leaf = new Path2D.Double();
                leaf.moveTo(5, 18);
                leaf.curveTo(1, 9, 10, 3, 21, 3);
                leaf.curveTo(21, 14, 15, 23, 5, 18);
                leaf.closePath();
                canvas.draw(leaf);
                Path2D stem = new Path2D.Double();
                stem.moveTo(3, 21);
                stem.curveTo(8, 13, 11, 10, 17, 7);
                canvas.draw(stem);
                line(canvas, 9, 14, 9, 9);
                line(canvas, 12, 11, 17, 12);
                break;
            default:
                break;
        }
        canvas.dispose();
    }

    private static void line(Graphics2D canvas, double x1, double y1, double x2, double y2) {
        canvas.draw(new Line2D.Double(x1, y1, x2, y2));
    }
}
