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
    public enum Kind { PLUS, ORDERS, RECEIPT, SETTINGS, LEAF, BURGER, TRAY, DRINK, SEARCH, REFRESH, ARROW_RIGHT }

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

    public AppIcon withColor(Color tint) { return new AppIcon(kind, size, tint); }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.translate(x, y);
        canvas.scale(size / 24.0, size / 24.0);
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.setColor(color);
        switch (kind) {
            case BURGER:
                Path2D bun = new Path2D.Double();
                bun.moveTo(3, 10);
                bun.curveTo(3, 1, 21, 1, 21, 10);
                bun.closePath();
                canvas.draw(bun);
                line(canvas, 8, 7, 8.5, 6.5);
                line(canvas, 13, 6, 13.5, 5.5);
                line(canvas, 17, 8, 17.5, 7.5);
                line(canvas, 3, 13, 7, 14);
                line(canvas, 7, 14, 12, 12);
                line(canvas, 12, 12, 17, 14);
                line(canvas, 17, 14, 21, 13);
                canvas.draw(new RoundRectangle2D.Double(3, 16, 18, 4, 3, 3));
                break;
            case TRAY:
                canvas.draw(new java.awt.geom.Arc2D.Double(4, 6, 16, 16, 0, 180, java.awt.geom.Arc2D.OPEN));
                line(canvas, 2, 15, 22, 15);
                line(canvas, 5, 19, 19, 19);
                line(canvas, 12, 4, 12, 6);
                break;
            case DRINK:
                Path2D cup = new Path2D.Double();
                cup.moveTo(6, 8); cup.lineTo(18, 8); cup.lineTo(16, 21);
                cup.lineTo(8, 21); cup.closePath();
                canvas.draw(cup);
                line(canvas, 5, 8, 19, 8);
                line(canvas, 12, 12, 14, 3);
                line(canvas, 14, 3, 19, 3);
                break;
            case SEARCH:
                canvas.draw(new Ellipse2D.Double(4, 3, 12, 12));
                line(canvas, 14, 14, 21, 21);
                break;
            case REFRESH:
                canvas.draw(new java.awt.geom.Arc2D.Double(4, 4, 16, 16, 40, 285, java.awt.geom.Arc2D.OPEN));
                line(canvas, 19, 4, 19, 9);
                line(canvas, 14, 9, 19, 9);
                break;
            case ARROW_RIGHT:
                line(canvas, 4, 12, 20, 12);
                line(canvas, 15, 7, 20, 12);
                line(canvas, 15, 17, 20, 12);
                break;
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
