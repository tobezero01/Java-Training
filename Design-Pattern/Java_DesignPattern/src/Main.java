import checkout.CheckoutProcesser;
import checkout.CodCheckoutProcessor;
import checkout.PaypalCheckoutProcessor;
import domain.Order;
import domain.PaymentMethod;
import domain.Receipt;
import singleton.AppConfig;

import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        AppConfig cfg = AppConfig.get();                 // Singleton
        cfg.set("paypal.feePercent", "0.05");            // 5%
        cfg.set("cod.feeFlat", "15000");


        // --- Tạo order mẫu ---
        Order order1 = new Order(1001L, new BigDecimal("125000.00"), "VND", PaymentMethod.PAYPAL);
        Order order2 = new Order(1002L, new BigDecimal("249000.00"), "VND", PaymentMethod.COD);


        // --- Chọn Creator theo phương thức thanh toán (Factory Method) ---
        CheckoutProcesser p1 = switch (order1.method()) {
            case PAYPAL -> new PaypalCheckoutProcessor();
            case COD    -> new CodCheckoutProcessor();
        };
        CheckoutProcesser p2 = switch (order2.method()) {
            case PAYPAL -> new PaypalCheckoutProcessor();
            case COD    -> new CodCheckoutProcessor();
        };

        Receipt r1 = p1.process(order1);
        Receipt r2 = p2.process(order2);

        System.out.println(r1.pretty());
        System.out.println(r2.pretty());

        // --- Chứng minh Singleton thật sự là 1 instance ---
        System.out.println("AppConfig is singleton? " + (cfg == AppConfig.get()));

        // --- Thử đổi config động và checkout lại (thấy phí thay đổi) ---
        cfg.set("paypal.feePercent", "0.02"); // giảm phí còn 2%
        Receipt r3 = new PaypalCheckoutProcessor().process(order1);
        System.out.println("[After fee change]\n" + r3.pretty());
    }
}