package Core_Basic.OOP_Exception.Domain.notify;

public final class ConsoleNotifier implements Notifier {
    @Override public void send(String message) {
        if (!Notifier.valid(message)) return;
        System.out.println("[NOTIFY] " + message);
    }
}
