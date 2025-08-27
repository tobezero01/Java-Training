package singleton;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AppConfig {
    private final Map<String , String> props = new ConcurrentHashMap<>();

    private AppConfig() {
        props.put("paypal.feePercent", "0.3");
        props.put("cod.feeFlat", "20000");

    }

    private static class Holder{ static final AppConfig I = new AppConfig();}

    public static AppConfig get() {return Holder.I;}

    public void set(String key, String value){ props.put(key, value);}
    public String get(String key) { return props.get(key); }

    public BigDecimal getMoney(String key, String def) {
        return new BigDecimal(props.getOrDefault(key, def));
    }
    public BigDecimal getPercent(String key, String def) {
        return new BigDecimal(props.getOrDefault(key, def));
    }
}
