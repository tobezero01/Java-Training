package payment;

import domain.Order;
import domain.Receipt;

public interface PaymentGateway {
    Receipt charge(Order order);
    String name();
}
