package Core_Basic.OOP_Exception.Domain.payment;

import Core_Basic.OOP_Exception.Domain.order.Order;
import Core_Basic.OOP_Exception.exception.PaymentFailedException;

public abstract class PaymentProcessor {
    public final void process(Order order) throws PaymentFailedException {
        precheck(order);
        String auth = authorize(order);
        try {
            capture(auth, order);
            receipt(order);
        } catch (Exception e) {
            throw new PaymentFailedException("Capture failed", e);
        }
    }
    protected void precheck(Order order){ /* validate order state, inventory... */ }
    protected abstract String authorize(Order order) throws PaymentFailedException;
    protected abstract void capture(String authorization, Order order) throws Exception;
    protected void receipt(Order order){ /* default: no-op */ }
}
