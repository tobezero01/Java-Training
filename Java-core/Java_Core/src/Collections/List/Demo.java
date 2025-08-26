package Collections.List;

import java.util.*;

public class Demo {
    public static void main(String[] args) {
        // 1) Tạo list bất biến
        List<String> names = List.of("An", "Bình", "Chi");

        // 2) Tạo list mutable từ mảng
        List<Integer> a = new ArrayList<>(Arrays.asList(1,2,3));

        // 3) Xóa tất cả số chẵn một cách an toàn
        a.removeIf(x -> (x & 1) == 0);

        List<String> fixed = List.of("An", "Bình", "Chi");
        System.out.println("Unmodifiable: " + fixed);

        // List mutable từ mảng ban đầu
        List<Integer> nums = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
        nums.add(9);                  // thêm cuối
        nums.add(2, 6);               // chèn 6 vào vị trí index = 2
        nums.remove(Integer.valueOf(1)); // xóa giá trị 1 (không phải xóa theo index!)
        nums.removeIf(x -> x % 2 == 0);  // xóa các số chẵn
        nums.sort(Comparator.naturalOrder()); // sắp xếp tăng dần

        int pos = Collections.binarySearch(nums, 5); // tìm 5 (yêu cầu đã sort)

        System.out.println("List hiện tại: " + nums);
        System.out.println("Vị trí của 5 (binarySearch): " + pos);
        System.out.println("contains(9)? " + nums.contains(9));
    }
}
