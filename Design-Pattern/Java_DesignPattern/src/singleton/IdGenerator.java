package singleton;

import java.util.concurrent.atomic.AtomicLong;

public enum IdGenerator {
    INSTANCE;
    private final AtomicLong seq = new AtomicLong(0L);

    public String next(String prefix) {
        long n = seq.incrementAndGet();
        return prefix + "-" + System.currentTimeMillis() + "-" + n;
    }
}
