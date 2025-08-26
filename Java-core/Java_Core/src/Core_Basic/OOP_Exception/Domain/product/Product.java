package Core_Basic.OOP_Exception.Domain.product;

import Core_Basic.OOP_Exception.core.Money;

import java.util.Objects;

public abstract class Product {
    private final String id;
    private final String name;
    private final Money price;

    protected Product(String id, String name, Money price) {
        if (name==null || name.isBlank()) throw new IllegalArgumentException("name blank");
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.price = Objects.requireNonNull(price);
    }
    public final String id(){ return id; }
    public final String name(){ return name; }
    public final Money price(){ return price; }

    /** hook cho subclass: thể hiện sự khác biệt (ví dụ VAT khác nhau) */
    public abstract Money tax();

    public Money priceWithTax(){ return price.plus(tax()); }

    @Override public String toString(){ return "%s(%s) %s".formatted(getClass().getSimpleName(), id, name); }
}
