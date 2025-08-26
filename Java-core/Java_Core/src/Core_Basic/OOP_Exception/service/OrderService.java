package Core_Basic.OOP_Exception.service;

import Core_Basic.OOP_Exception.Domain.notify.Notifier;
import Core_Basic.OOP_Exception.Domain.order.Order;
import Core_Basic.OOP_Exception.Domain.order.OrderItem;
import Core_Basic.OOP_Exception.Domain.payment.PaymentProcessor;
import Core_Basic.OOP_Exception.Domain.pricing.DiscountPolicy;
import Core_Basic.OOP_Exception.core.IdGenerator;
import Core_Basic.OOP_Exception.core.Money;
import Core_Basic.OOP_Exception.exception.PaymentFailedException;

public final class OrderService {
    private final Notifier notifier;
    public OrderService(Notifier notifier){ this.notifier = notifier; }

    public Order createOrder(){ return new Order(IdGenerator.next()); }

    public void addItem(Order order, OrderItem item){ order.addItem(item); }

    public Money checkout(Order order, DiscountPolicy discount, PaymentProcessor payment)
            throws PaymentFailedException {
        Money total = order.total(discount);
        payment.process(order);
        order.markPaid();
        notifier.info("Order %s paid: %s".formatted(order.id(), total));
        return total;
    }
}
