package checkout;

import payment.PaymentGateway;
import payment.gateway.PaypalGateway;

public final class PaypalCheckoutProcessor extends CheckoutProcesser {
    @Override protected PaymentGateway createGateway() { return new PaypalGateway(); }
}
