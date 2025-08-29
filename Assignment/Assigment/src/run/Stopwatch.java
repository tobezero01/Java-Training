package run;

/// Bộ đếm thời gian
public class Stopwatch {
    private long startNanos;

    private Stopwatch(long startNanos) {
        this.startNanos = startNanos;
    }

    /// Tạo và bắt đầu đếm ngay.
    public static Stopwatch startNew() {
        return new Stopwatch(System.nanoTime());
    }

    /// Thời gian đã trôi qua (ns) kể từ khi start.
    public long elapsedNanos() {
        return System.nanoTime() - startNanos;
    }

    /// Thời gian đã trôi qua (ms).
    public long elapsedMillis() {
        return elapsedNanos() / 1_000_000L;
    }

    /// Thời gian đã trôi qua (s).
    public double elapsedSeconds() {
        return elapsedNanos() / 1_000_000_000.0;
    }
}
