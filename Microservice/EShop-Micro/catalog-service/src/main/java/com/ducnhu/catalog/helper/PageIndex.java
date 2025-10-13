package com.ducnhu.catalog.helper;

import java.util.List;

/** (Giải thích) Dùng để cache MỎNG: chỉ IDs + metadata trang */
public final class PageIndex {
    public List<Integer> ids;  // danh sách ID của trang
    public int page;           // trang 1-based
    public int size;           // kích thước trang (20)
    public long total;         // tổng số bản ghi
    public int totalPages;     // tổng số trang

    public PageIndex() {}
    public PageIndex(List<Integer> ids, int page, int size, long total, int totalPages){
        this.ids=ids; this.page=page; this.size=size; this.total=total; this.totalPages=totalPages;
    }
}