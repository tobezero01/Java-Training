package Core_Basic.OOP_Exception.core;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money implements Comparable<Money>{
    //giá trị tiền tệ chính xác
    private final BigDecimal amount;

    // hàm tạo money và chuẩn hóa đối tượng (đa hình)
    public static Money of(double v) {return new Money(BigDecimal.valueOf(v));}
    public static Money of(BigDecimal v){ return new Money(v); }

    private Money(BigDecimal amount) {
        this.amount = amount.stripTrailingZeros();
    }
    // đọc giá trị bên trong
    public BigDecimal value(){ return amount; }

    //toán học tiền tệ, mỗi phép trả về đối tượng mới → immutable.
    public Money plus(Money other){ return of(this.amount.add(other.amount)); }
    public Money minus(Money other){ return of(this.amount.subtract(other.amount)); }
    public Money times(double rate){ return of(this.amount.multiply(BigDecimal.valueOf(rate))); }

    @Override public int compareTo(Money o){ return this.amount.compareTo(o.amount); }
    @Override public boolean equals(Object o){ return o instanceof Money m && amount.compareTo(m.amount)==0; }
    @Override public int hashCode(){ return Objects.hash(amount); }
    @Override public String toString(){ return amount.toPlainString(); }
}
