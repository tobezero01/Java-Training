package Collections.Set;

import java.util.*;

public class Demo {
    record Person(String name, String city, int age) {}
    enum Role { ADMIN, USER, GUEST }
    enum Perm { READ, WRITE, EXEC }

    public static void main(String[] args) {
        demoHashSetOps();
        demoLinkedHashSetDedup();
        demoTreeSetSortedRange();
        demoEnumSetOps();
    }

    // HashSet: union/intersection/difference
    static void demoHashSetOps() {
        System.out.println("\n== HashSet Toán tử tập hợp ==");
        Set<Integer> a = new HashSet<>(List.of(1,2,3));
        Set<Integer> b = new HashSet<>(List.of(3,4,5));

        Set<Integer> union = new HashSet<>(a); union.addAll(b);
        Set<Integer> inter = new HashSet<>(a); inter.retainAll(b);
        Set<Integer> diff  = new HashSet<>(a); diff.removeAll(b);

        System.out.println("a=" + a + ", b=" + b);
        System.out.println("union=" + union + ", inter=" + inter + ", diff=" + diff);
    }

    // LinkedHashSet: khử trùng lặp nhưng GIỮ thứ tự chèn
    static void demoLinkedHashSetDedup() {
        System.out.println("\n== LinkedHashSet Dedup (giữ thứ tự) ==");
        List<Integer> input = List.of(3,1,2,3,1,4,2,5);
        List<Integer> dedup = new ArrayList<>(new LinkedHashSet<>(input));
        System.out.println("Input   : " + input);
        System.out.println("Dedup   : " + dedup);
    }

    // TreeSet: sắp xếp với Comparator + range query
    static void demoTreeSetSortedRange() {
        System.out.println("\n== TreeSet Sorted + Range ==");
        // Sắp theo độ dài rồi theo thứ tự tự nhiên
        TreeSet<String> ts = new TreeSet<>(Comparator
                .comparingInt(String::length)
                .thenComparing(Comparator.naturalOrder()));

        ts.addAll(List.of("b", "aa", "ccc", "bb", "dddd", "a"));
        System.out.println("TreeSet (len, then alpha): " + ts);
        System.out.println("floor(\"zzzz\") = " + ts.floor("zzzz"));   // phần tử dài nhất <= len 4
        System.out.println("ceiling(\"ba\") = " + ts.ceiling("ba"));
        // Lấy đoạn có độ dài 2 (từ "aa" tới "zz" cùng chiều so sánh)
        SortedSet<String> len2 = ts.subSet("aa", true, "zz", true);
        System.out.println("subSet(len==2) ~ " + len2);
    }

    // EnumSet: set tối ưu cho enum
    static void demoEnumSetOps() {
        System.out.println("\n== EnumSet ==");
        EnumSet<Perm> p = EnumSet.of(Perm.READ, Perm.WRITE);
        p.add(Perm.EXEC);
        EnumSet<Perm> rw = EnumSet.of(Perm.READ, Perm.WRITE);
        EnumSet<Perm> onlyExec = EnumSet.complementOf(rw); // {EXEC}
        System.out.println("Perms: " + p + " | complementOf(READ,WRITE) = " + onlyExec);
    }
}
