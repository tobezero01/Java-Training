package Core_Basic.OOP_Exception;

import Core_Basic.OOP_Exception.Domain.notify.ConsoleNotifier;
import Core_Basic.OOP_Exception.Domain.order.Order;
import Core_Basic.OOP_Exception.Domain.order.OrderItem;
import Core_Basic.OOP_Exception.Domain.payment.CreditPayment;
import Core_Basic.OOP_Exception.Domain.payment.PaymentProcessor;
import Core_Basic.OOP_Exception.Domain.pricing.CompositeDiscount;
import Core_Basic.OOP_Exception.Domain.pricing.DiscountPolicy;
import Core_Basic.OOP_Exception.Domain.pricing.PercentageDiscount;
import Core_Basic.OOP_Exception.Domain.product.Book;
import Core_Basic.OOP_Exception.Domain.product.DigitalProduct;
import Core_Basic.OOP_Exception.repo.ProductRepository;
import Core_Basic.OOP_Exception.service.CatalogService;
import Core_Basic.OOP_Exception.service.OrderService;

public class Main {
    //repo → services → seed dữ liệu → tạo order → discount chain → chọn PaymentProcessor → checkout
    public static void main(String[] args) throws Exception {
        // Setup
        var repo = new ProductRepository();
        var catalog = new CatalogService(repo);
        var notifier = new ConsoleNotifier();
        var orderService = new OrderService(notifier);

        // Seed products
        Book b1 = catalog.addBook("Clean Code", 25.0, "Robert C. Martin");
        DigitalProduct d1 = catalog.addDigital("JetBrains License", 99.0);

        // Create order
        Order order = orderService.createOrder();
        orderService.addItem(order, new OrderItem(b1, 2));
        orderService.addItem(order, new OrderItem(d1, 1));

        // Pricing strategy: 5% + 3% discount chained
        DiscountPolicy discount = new CompositeDiscount(
                new PercentageDiscount(0.05),
                new PercentageDiscount(0.03)
        );

        // Choose payment processor polymorphically
        PaymentProcessor payment = new CreditPayment(); // or new EWalletPayment();

        // Checkout
        var total = orderService.checkout(order, discount, payment);
        System.out.println("Subtotal: " + order.subtotal());
        System.out.println("Total after discount: " + total);

        // Show catalog sorted
        System.out.println("Products (price asc): " + repo.findSortedByPriceAsc());
    }
}
