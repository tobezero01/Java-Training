package Core_Basic.OOP_Exception.exception;

public class PaymentFailedException extends Exception {
    public PaymentFailedException(String m){ super(m); }
    public PaymentFailedException(String m, Throwable c){ super(m,c); }
}
