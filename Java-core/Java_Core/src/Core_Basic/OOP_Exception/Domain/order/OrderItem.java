package Core_Basic.OOP_Exception.Domain.order;

import Core_Basic.OOP_Exception.Domain.product.Product;
import Core_Basic.OOP_Exception.core.Money;

import java.util.Objects;

public final class OrderItem {
    private final Product product;
    private final int quantity;

    public OrderItem(Product product, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity>0");
        this.product = Objects.requireNonNull(product);
        this.quantity = quantity;
    }
    public Product product(){ return product; }
    public int quantity(){ return quantity; }
    public Money lineTotal(){ return product.priceWithTax().times(quantity); }
}
