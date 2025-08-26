package Core_Basic.OOP_Exception.Domain.pricing;

import Core_Basic.OOP_Exception.core.Money;

@FunctionalInterface
public interface DiscountPolicy {
    Money apply(Money subtotal);

    default DiscountPolicy then(DiscountPolicy next){
        return sub -> next.apply(apply(sub)); // chain
    }

    static DiscountPolicy none(){ return sub -> Money.of(0); }
}
