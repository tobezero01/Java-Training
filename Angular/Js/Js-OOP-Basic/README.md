## Kiến trúc lớp
- `Book`: mô hình hoá một cuốn sách (id, title, author, year, isRead).
- `BookManager`: quản lý danh sách sách (CRUD, lọc, thống kê), giao tiếp `Storage`.
- `Storage`: bọc `localStorage` để lưu/đọc.
- `UI`: điều khiển DOM, render danh sách, uỷ quyền sự kiện (delegation).
