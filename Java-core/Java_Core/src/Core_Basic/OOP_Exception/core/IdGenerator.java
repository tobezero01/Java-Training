package Core_Basic.OOP_Exception.core;

import java.util.concurrent.atomic.AtomicLong;

public final class IdGenerator {
    private static final AtomicLong SEQ = new AtomicLong(1000);
    private IdGenerator(){}
    public static String next(){ return "ID-" + SEQ.getAndIncrement(); }
}
