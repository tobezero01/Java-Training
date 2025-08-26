package Core_Basic.OOP_Exception.Domain.payment;

import Core_Basic.OOP_Exception.Domain.order.Order;
import Core_Basic.OOP_Exception.exception.PaymentFailedException;

public final class CreditPayment extends PaymentProcessor {
    @Override protected String authorize(Order order) throws PaymentFailedException {
        // giả lập: luôn ok
        return "AUTH-CC-" + order.id();
    }
    @Override protected void capture(String auth, Order order){
        // giả lập: in ra màn hình
        System.out.println("[CC] captured " + auth + " for order " + order.id());
    }
}
