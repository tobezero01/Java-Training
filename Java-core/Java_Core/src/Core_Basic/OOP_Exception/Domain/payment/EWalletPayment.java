package Core_Basic.OOP_Exception.Domain.payment;

import Core_Basic.OOP_Exception.Domain.order.Order;
import Core_Basic.OOP_Exception.exception.PaymentFailedException;

public final class EWalletPayment extends PaymentProcessor {
    @Override protected String authorize(Order order) throws PaymentFailedException {
        return "AUTH-WALLET-" + order.id();
    }
    @Override protected void capture(String auth, Order order){
        System.out.println("[WALLET] captured " + auth + " for order " + order.id());
    }
}
