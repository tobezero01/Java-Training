package payment.gateway;

import domain.Order;
import domain.PaymentStatus;
import domain.Receipt;
import payment.PaymentGateway;
import singleton.AppConfig;
import singleton.IdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PaypalGateway implements PaymentGateway {
    @Override
    public Receipt charge(Order order) {
        BigDecimal feePercent = AppConfig.get().getPercent("paypal.feePercent", "0.03");
        BigDecimal fee = percentOf(order.amount(), feePercent);
        BigDecimal net = order.amount().subtract(fee).max(BigDecimal.ZERO);
        String txId = IdGenerator.INSTANCE.next("PP");
        return new Receipt(
                order.id(), name(), txId, PaymentStatus.SUCCESS,
                order.amount(), fee, net,
                "PayPal charged with fee " + toPercentString(feePercent)
        );
    }

    @Override
    public String name() {
        return "PAYPAL";
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal p) {
        // p = 0.05 -> 5%
        return amount.multiply(p).setScale(2, RoundingMode.HALF_UP);
    }
    private String toPercentString(BigDecimal p) {
        return p.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }
}
