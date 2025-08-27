package checkout;

import payment.PaymentGateway;
import payment.gateway.CodGateway;

public final class CodCheckoutProcessor extends CheckoutProcesser {
    @Override protected PaymentGateway createGateway() { return new CodGateway(); }
}
