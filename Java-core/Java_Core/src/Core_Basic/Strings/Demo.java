package Core_Basic.Strings;

import java.nio.charset.StandardCharsets;
import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

public class Demo {
    public static void main(String[] args) {
        System.out.println("=== TẠO STRING & STRING POOL ===");
        String a = "Hello";
        String b = "Hello";
        String c = new String("Hello");          // thuộc heap
        System.out.println("a == b ? " + (a == b));      // true (cùng pool)
        System.out.println("a == c ? " + (a == c));      // false (khác object)
        System.out.println("a.equals(c) ? " + a.equals(c)); // true

        // intern(): đưa chuỗi runtime vào pool
        String run = new String("Hello");
        System.out.println("run.intern() == a ? " + (run.intern() == a)); // true

        System.out.println("\n=== TÍNH BẤT BIẾN (IMMUTABLE) & NỐI CHUỖI ===");
        String s = "Java";
        String s2 = s.replace("J", "L"); // sinh chuỗi mới
        System.out.println("s = " + s + ", s2 = " + s2);

        String x = "a" + "b"; // "ab" đặt vào pool
        String part = "b";
        String y = "a" + part; // object mới, không tự vào pool
        System.out.println("x == \"ab\" ? " + (x == "ab")); // true
        System.out.println("y == \"ab\" ? " + (y == "ab")); // false
        System.out.println("y.intern()==\"ab\" ? " + (y.intern() == "ab")); // true

        System.out.println("\n=== SO SÁNH CHUỖI ===");
        String h1 = "hello";
        String h2 = "HeLLo";
        System.out.println("equals: " + h1.equals(h2));                 // false
        System.out.println("equalsIgnoreCase: " + h1.equalsIgnoreCase(h2)); // true
        System.out.println("compareTo(\"world\"): " + h1.compareTo("world"));
        System.out.println("regionMatches(ignoreCase=true, \"ABCDEF\",\"ABC\"): " +
                "abcdef".regionMatches(true, 0, "ABC", 0, 3)); // true
        System.out.println("(\"ok\" == new String(\"ok\")) ? " + ("ok" == new String("ok"))); // false

        System.out.println("\n=== STRINGBUILDER TRONG VÒNG LẶP (HIỆU NĂNG) ===");
        StringBuilder sb = new StringBuilder(64);
        for (int i = 1; i <= 5; i++) sb.append(i).append(",");
        String joined = sb.toString();
        System.out.println("Builder result: " + joined); // "1,2,3,4,5,"


        System.out.println("\n=== CHUYỂN BYTE <-> STRING (LUÔN CHỈ ĐỊNH CHARSET) ===");
        byte[] utf8 = "Xin chào".getBytes(StandardCharsets.UTF_8);
        String fromBytes = new String(utf8, StandardCharsets.UTF_8);
        System.out.println("fromBytes = " + fromBytes);

        System.out.println("\n=== ĐỊNH DẠNG, GHÉP CHUỖI, LẶP ===");
        String formatted = String.format("Hi %s, số: %,d", "Duc", 1234567);
        String joined2 = String.join(" | ", "red", "green", "blue");
        String repeated = "ha".repeat(3);
        System.out.println(formatted);
        System.out.println(joined2);     // "red | green | blue"
        System.out.println(repeated);    // "hahaha"



    }
}
