package Core_Basic.OOP_Exception.Domain.pricing;

import Core_Basic.OOP_Exception.core.Money;

public final class CompositeDiscount implements DiscountPolicy {
    private final DiscountPolicy delegate;
    public CompositeDiscount(DiscountPolicy... policies){
        DiscountPolicy d = DiscountPolicy.none();
        for (DiscountPolicy p : policies) d = d.then(p);
        this.delegate = d;
    }
    @Override public Money apply(Money subtotal){ return delegate.apply(subtotal); }
}