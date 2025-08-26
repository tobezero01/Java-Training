package Core_Basic.OOP_Exception.Domain.notify;

public interface Notifier {
    void send(String message);

    default void info(String msg){ send("[INFO] " + msg); }
    static boolean valid(String msg){ return msg != null && !msg.isBlank(); }
}
