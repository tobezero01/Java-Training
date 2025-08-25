package Core_Basic.PrimitiveAndObject_Datatype;

public class Demo {
    int primitiveInt;          // 0
    Integer wrapperInt;        // null

    boolean primitiveBool;     // false
    Boolean wrapperBool;       // null

    public static void main(String[] args) {

        Demo demo = new Demo();

        System.out.println("=== Giá trị mặc định ===");
        System.out.println("Primitive Int = " + demo.primitiveInt);   // 0
        System.out.println("Wrapper Int   = " + demo.wrapperInt);     // null
        System.out.println("Primitive Bool= " + demo.primitiveBool);  // false
        System.out.println("Wrapper Bool  = " + demo.wrapperBool);    // null

        System.out.println("\n=== Local variable (phải khởi tạo) ===");
        int localPrimitive = 5;
        Integer localWrapper = 5; // tại đây sẽ xảy ra autoboxing từ primitive
        System.out.println("Local Primitive = " + localPrimitive);
        System.out.println("Local Wrapper   = " + localWrapper);

        System.out.println("\n=== So sánh ===");
        int a = 100;
        Integer b = 100;
        Integer c = 100;
        Integer d = 200;
        Integer e = 200;

        System.out.println("a == b ? " + (a == b));          // true
        System.out.println("b == c ? " + (b == c));          // true
        System.out.println("d == e ? " + (d == e));          // false do so sánh tham chiếu
        System.out.println("d.equals(e) ? " + d.equals(e));  // true

        System.out.println("\n=== Autoboxing & Unboxing ===");
        Integer boxed = a;
        int unboxed = boxed;
        System.out.println("boxed (Integer) = " + boxed);
        System.out.println("unboxed (int)   = " + unboxed);

        System.out.println("\n=== Test Null Wrapper ===");
        try {
            Integer nullWrapper = null;
            int unsafe = nullWrapper; // auto-unbox -> NPE
            System.out.println(unsafe);
        } catch (NullPointerException ex) {
            System.out.println("Unboxing null gây ra NullPointerException!");
        }

    }
}
