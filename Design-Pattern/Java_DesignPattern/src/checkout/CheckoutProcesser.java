package checkout;

import domain.Order;
import domain.Receipt;
import payment.PaymentGateway;

import java.math.BigDecimal;

public abstract class CheckoutProcesser {
    protected abstract PaymentGateway createGateway();

    public final Receipt process(Order order) {
        validate(order);
        PaymentGateway gw = createGateway();
        Receipt r = gw.charge(order);
        postActions(r);  // gửi event/log/audit...
        return r;
    }

    protected void validate(Order order) {
        if (order.amount() == null || order.amount().compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Invalid amount");
    }
    protected void postActions(Receipt r) {
        System.out.println("[AUDIT] tx=" + r.toString().hashCode());
    }

}
