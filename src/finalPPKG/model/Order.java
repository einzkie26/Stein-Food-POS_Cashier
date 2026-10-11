package finalPPKG.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** An immutable order line. All money is calculated here, independently of the UI. */
public final class Order {
    public static final int MAX_QUANTITY = 9_999;
    public static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("999999999.99");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final String dish;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountPercent;
    private final BigDecimal subtotal;
    private final BigDecimal discountAmount;
    private final BigDecimal total;

    public Order(String dish, int quantity, BigDecimal unitPrice, BigDecimal discountPercent) {
        if (dish == null || dish.isBlank()) {
            throw new IllegalArgumentException("Choose a dish or enter its name.");
        }
        if (dish.indexOf(':') >= 0 || dish.indexOf('\n') >= 0 || dish.indexOf('\r') >= 0
                || dish.indexOf('\u2028') >= 0 || dish.indexOf('\u2029') >= 0) {
            throw new IllegalArgumentException("A dish name cannot contain colons or line breaks.");
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be from 1 to " + MAX_QUANTITY + ".");
        }
        requireDecimal(unitPrice, "Unit price");
        requireDecimal(discountPercent, "Discount");
        if (unitPrice.signum() < 0 || unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new IllegalArgumentException("Unit price must be from 0 to 999,999,999.99.");
        }
        if (discountPercent.signum() < 0 || discountPercent.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("Discount must be from 0 to 100 percent.");
        }

        this.dish = dish.strip();
        this.quantity = quantity;
        this.unitPrice = money(unitPrice);
        this.discountPercent = discountPercent.stripTrailingZeros();
        this.subtotal = money(this.unitPrice.multiply(BigDecimal.valueOf(quantity)));
        this.discountAmount = money(subtotal.multiply(this.discountPercent).divide(ONE_HUNDRED));
        this.total = money(subtotal.subtract(discountAmount));
    }

    private static void requireDecimal(BigDecimal value, String label) {
        if (value == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (value.precision() > 18 || value.scale() > 6 || value.scale() < -9) {
            throw new IllegalArgumentException(label + " must be a number with at most 6 decimal places.");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public String getDish() { return dish; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTotal() { return total; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Order order)) return false;
        return quantity == order.quantity && dish.equals(order.dish)
                && unitPrice.equals(order.unitPrice) && discountPercent.equals(order.discountPercent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dish, quantity, unitPrice, discountPercent);
    }

    @Override
    public String toString() {
        return dish + " × " + quantity + " — PHP " + total.toPlainString();
    }
}
