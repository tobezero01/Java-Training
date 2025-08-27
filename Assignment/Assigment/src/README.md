# LogSearch – Bộ lọc log (Java Core)

## 🎯 Mục tiêu
- **Đầu vào**: 1 file log rất lớn (≥ 500k dòng) theo chuẩn:
- **Bộ lọc**: `level` (một/nhiều), `from/to` (khoảng thời gian), `contains` (từ khóa trong message).
- **Đầu ra**: In ra **stdout** hoặc ghi ra file **.txt** (chỉ các dòng khớp).

---

## 🧠 Ý tưởng cốt lõi
- **Không** gom kết quả vào danh sách; **ghi ngay** từng dòng khớp (stdout / `BufferedWriter`).
- Dùng **Regex** cố định để tách 4 phần; parse thời gian bằng **`OffsetDateTime`**.
- Bộ lọc kết hợp theo **AND**: `(level) ∧ (from/to) ∧ (contains)`.
- Dùng BufferedReader/Writer để stream file lớn (>500k dòng) — tiết kiệm RAM.
- Regex tách 4 phần, OffsetDateTime parse thời gian ISO-8601 (có múi giờ).
- Bộ lọc Level/Time/Contains đều có thể bỏ trống để “không lọc”.
- contains hỗ trợ nhiều từ khóa (OR), không phân biệt hoa/thường.
- Báo cáo số dòng đọc, số dòng sai format (bị bỏ qua), số dòng được ghi.



