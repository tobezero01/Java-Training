package domain;

import java.math.BigDecimal;

public final class Receipt {
    private final long orderId;
    private final String gateway;
    private final String txId;
    private final PaymentStatus status;
    private final BigDecimal gross;  // số tiền gốc
    private final BigDecimal fee;    // phí gateway
    private final BigDecimal net;    // số tiền thực nhận
    private final String message;

    public Receipt(long orderId, String gateway, String txId, PaymentStatus status, BigDecimal gross, BigDecimal fee, BigDecimal net, String message) {
        this.orderId = orderId;
        this.gateway = gateway;
        this.txId = txId;
        this.status = status;
        this.gross = gross;
        this.fee = fee;
        this.net = net;
        this.message = message;
    }

    public String pretty() {
        return """
                ---- RECEIPT ----
                orderId: %d
                gateway: %s
                txId   : %s
                status : %s
                gross  : %s
                fee    : %s
                net    : %s
                note   : %s
                """.formatted(orderId, gateway, txId, status, gross, fee, net, message);
    }
}
