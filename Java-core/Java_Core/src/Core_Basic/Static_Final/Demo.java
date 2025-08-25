package Core_Basic.Static_Final;

public class Demo {
    public static final int MAX_NAME_LEN = 50;

    // Biến static chia sẻ trạng thái
    private static int liveUsers = 0;

    // Trường final: mỗi object có, gán đúng 1 lần
    private final String name;

    // Static block: chạy 1 lần lúc class init
    static { System.out.println("User class initialized"); }

    public Demo(String name) {
        if (name == null || name.length() > MAX_NAME_LEN)
            throw new IllegalArgumentException("Invalid name");
        this.name = name;
        liveUsers++;
    }

    // Phương thức static tiện ích
    public static int getLiveUsers() { return liveUsers; }

    // Getter: name là final -> không đổi sau khi tạo
    public final String getName() { return name; } // final method: không cho override

    // Static nested class (không cần tham chiếu User.this)
    public static class Builder {
        private String name;
        public Builder name(String n){ this.name = n; return this; }
        public Demo build(){ return new Demo(name); }
    }
}
