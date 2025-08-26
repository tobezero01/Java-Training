package Core_Basic.OOP_Exception.Domain.pricing;

import Core_Basic.OOP_Exception.core.Money;

public final class PercentageDiscount implements DiscountPolicy {
    private final double rate; // 0..1
    public PercentageDiscount(double rate) {
        if (rate < 0 || rate > 1) throw new IllegalArgumentException("0..1");
        this.rate = rate;
    }
    @Override public Money apply(Money subtotal){ return subtotal.times(rate); }
}
