package Core_Basic.OOP_Exception.core;

import java.util.Objects;
import java.util.function.Function;

public final class Result<T> {
    private final T value;
    private final String error;

    private Result(T v, String e) { value=v; error=e; }
    public static <T> Result<T> ok(T v){ return new Result<>(v,null); }
    public static <T> Result<T> fail(String e){ return new Result<>(null, Objects.requireNonNull(e)); }
    public boolean isOk(){ return error==null; }
    public T get(){ if(!isOk()) throw new IllegalStateException(error); return value; }
    public String error(){ return error; }
    public <U> Result<U> map(Function<T,U> f){ return isOk()? ok(f.apply(value)) : fail(error); }
}
