package Core_Basic.OOP_Exception.Domain.order;

import Core_Basic.OOP_Exception.Domain.pricing.DiscountPolicy;
import Core_Basic.OOP_Exception.core.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Order {
    private final String id;
    private final Instant createdAt = Instant.now();
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.NEW;

    public Order(String id) { this.id = Objects.requireNonNull(id); }

    public String id(){ return id; }
    public Instant createdAt(){ return createdAt; }
    public OrderStatus status(){ return status; }
    public void markPaid(){ this.status = OrderStatus.PAID; }
    void markFailed(){ this.status = OrderStatus.FAILED; }

    public void addItem(OrderItem item){ items.add(Objects.requireNonNull(item)); }
    public List<OrderItem> items(){ return List.copyOf(items); }

    public Money subtotal(){
        return items.stream().map(OrderItem::lineTotal)
                .reduce(Money.of(0), Money::plus);
    }

    public Money total(DiscountPolicy discount){
        Money s = subtotal();
        Money d = discount.apply(s);
        return s.minus(d);
    }
}
