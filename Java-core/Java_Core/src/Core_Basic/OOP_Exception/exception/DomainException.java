package Core_Basic.OOP_Exception.exception;

public class DomainException extends RuntimeException {
    public DomainException(String m){ super(m); }
    public DomainException(String m, Throwable c){ super(m,c); }
}