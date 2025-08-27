package domain;

import java.math.BigDecimal;

public record Order(long id, BigDecimal amount, String currency, PaymentMethod method) {
}
