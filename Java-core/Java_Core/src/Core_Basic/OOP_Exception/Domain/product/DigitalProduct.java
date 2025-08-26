package Core_Basic.OOP_Exception.Domain.product;

import Core_Basic.OOP_Exception.core.Money;

public final class DigitalProduct extends Product {
    public DigitalProduct(String id, String name, Money price) { super(id, name, price); }
    @Override public Money tax(){ return price().times(0.1); } // 10% VAT
}
