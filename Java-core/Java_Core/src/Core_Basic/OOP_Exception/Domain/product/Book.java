package Core_Basic.OOP_Exception.Domain.product;

import Core_Basic.OOP_Exception.core.Money;

public final class Book extends Product{
    private final String author;
    public Book(String id, String name, Money price, String author) {
        super(id, name, price);
        this.author = author;
    }
    public String author(){ return author; }
    @Override
    public Money tax() {
        return Money.of(0);
    }
}
