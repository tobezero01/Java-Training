package Collections.Map;

import java.util.*;

public class Demo {
    record Person(String name, String city, int age) {}
    enum Role { ADMIN, USER, GUEST }
    enum Perm { READ, WRITE, EXEC }

    public static void main(String[] args) {
        demoHashMapBasic();
        demoGroupingAndFrequency();
        demoTreeMapRange();
        demoEnumMap();
    }

    // HashMap cơ bản: put/get/remove/replace/compute/merge
    static void demoHashMapBasic() {
        System.out.println("\n== HashMap cơ bản ==");
        Map<String, Integer> ages = new HashMap<>(Map.of("An", 20, "Bình", 21));
        ages.put("Chi", 22);
        ages.putIfAbsent("An", 18); // không ghi đè
        System.out.println("Ages: " + ages);

        int ageAn = ages.getOrDefault("An", -1);
        System.out.println("An = " + ageAn + ", containsKey(Chi) = " + ages.containsKey("Chi"));

        ages.replace("Bình", 21, 23); // chỉ thay nếu giá trị hiện tại là 21
        ages.compute("Chi", (k, v) -> v == null ? 1 : v + 1); // tăng tuổi Chi
        ages.merge("An", 1, Integer::sum); // gộp (An += 1)
        System.out.println("Sau replace/compute/merge: " + ages);

        ages.remove("KhôngTồnTại");               // không làm gì
        ages.remove("An", 999);                   // chỉ xóa nếu value khớp
        System.out.println("Cuối cùng: " + ages);
    }

    // Nhóm dữ liệu (multimap) + đếm tần suất
    static void demoGroupingAndFrequency() {
        System.out.println("\n== Grouping & Frequency ==");
        List<Person> people = List.of(
                new Person("An", "HN", 20),
                new Person("Bình", "HCM", 21),
                new Person("Chi", "HN", 22),
                new Person("Dũng", "ĐN", 25),
                new Person("An", "HN", 20)
        );

        // Multimap với computeIfAbsent (Map<String, List<Person>>)
        Map<String, List<Person>> byCity = new HashMap<>();
        for (Person p : people) {
            byCity.computeIfAbsent(p.city(), k -> new ArrayList<>()).add(p);
        }
        System.out.println("Group by city (computeIfAbsent): " + byCity);

        // Tần suất từ
        String text = "a b a c b a d b c";
        Map<String,Integer> freq = new HashMap<>();
        for (String w : text.split("\\s+")) {
            freq.merge(w, 1, Integer::sum);
        }
        // In top-3 theo value giảm dần
        List<Map.Entry<String,Integer>> top3 = freq.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
                .limit(3).toList();
        System.out.println("Tần suất: " + freq + " | Top3: " + top3);
    }

    // TreeMap
    static void demoTreeMapRange() {
        System.out.println("\n== TreeMap Range ==");
        TreeMap<Integer,String> tm = new TreeMap<>();
        tm.put(10,"a"); tm.put(20,"b"); tm.put(30,"c");
        System.out.println("floorEntry(25) = " + tm.floorEntry(25));
        System.out.println("ceilingKey(21) = " + tm.ceilingKey(21));
        System.out.println("subMap [10..30) = " + tm.subMap(10, true, 30, false));
    }

    // EnumMap
    static void demoEnumMap() {
        System.out.println("\n== EnumMap ==");
        EnumMap<Role, Integer> quota = new EnumMap<>(Role.class);
        quota.put(Role.ADMIN, 1000);
        quota.put(Role.USER, 100);
        quota.put(Role.GUEST, 10);
        System.out.println("Quota: " + quota);
    }
}
