package payment.gateway;

import domain.Order;
import domain.PaymentStatus;
import domain.Receipt;
import payment.PaymentGateway;
import singleton.AppConfig;
import singleton.IdGenerator;

import java.math.BigDecimal;

public final class CodGateway implements PaymentGateway {
    @Override public String name() { return "CASH_ON_DELIVERY"; }

    @Override
    public Receipt charge(Order order) {
        BigDecimal flat = AppConfig.get().getMoney("cod.feeFlat", "20000");
        BigDecimal fee = flat.min(order.amount());
        BigDecimal net = order.amount().subtract(fee).max(BigDecimal.ZERO);
        String txId = IdGenerator.INSTANCE.next("COD");

        return new Receipt(
                order.id(), name(), txId, PaymentStatus.SUCCESS,
                order.amount(), fee, net,
                "COD fee flat " + flat.toPlainString() + " " + order.currency()
        );
    }
}
