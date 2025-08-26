package Collections.Array;

public class Demo {

    public static void main(String[] args) {
        int[] src = {1,2,3,4};
        // Sao chép nhanh (native)
        int[] dst = new int[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);

        // Nới kích thước
        int[] bigger = java.util.Arrays.copyOf(src, 10);

        // Điền
        java.util.Arrays.fill(dst, -1);

        // Set theo công thức
        java.util.Arrays.setAll(dst, i -> i * i);
    }

    // Binary Search
    // Tìm chỉ số đầu tiên i sao cho arr[i] >= target
    int lowerBound(int[] arr, int target) {
        int l = 0, r = arr.length; // [l, r)
        while (l < r) {
            int mid = l + (r - l) / 2;
            if (arr[mid] < target) l = mid + 1; else r = mid;
        }
        return l;
    }


    // ================ Đảo ngược/Quay mảng/Hoán vị kế tiếp ==============
    public static void reverse(int[] a, int l, int r) { // inclusive
        while (l < r) { int t = a[l]; a[l++] = a[r]; a[r--] = t; }
    }
    // Rotate phải k bước: reverse 3 lần
    public static void rotateRight(int[] a, int k) {
        k %= a.length; if (k == 0) return;
        reverse(a, 0, a.length-1);
        reverse(a, 0, k-1);
        reverse(a, k, a.length-1);
    }


    // ================= Hai con trỏ (Two pointers) & cửa sổ trượt (Sliding window) ============================
    // Loại bỏ trùng kề nhau trong mảng đã sort, trả về độ dài mới
    public static int dedupSorted(int[] a) {
        if (a.length == 0) return 0;
        int w = 1;
        for (int r = 1; r < a.length; r++) {
            if (a[r] != a[w-1]) a[w++] = a[r];
        }
        return w; // các phần tử hợp lệ nằm trong a[0..w-1]
    }

    // Cửa sổ trượt: tổng <= K, độ dài lớn nhất
    public static int longestSubarraySumLEK(int[] a, int K) {
        int best = 0, sum = 0, l = 0;
        for (int r = 0; r < a.length; r++) {
            sum += a[r];
            while (sum > K && l <= r) sum -= a[l++];
            best = Math.max(best, r - l + 1);
        }
        return best;
    }


}
