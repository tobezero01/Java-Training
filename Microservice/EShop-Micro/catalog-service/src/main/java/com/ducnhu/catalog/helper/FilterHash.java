package com.ducnhu.catalog.helper;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/** (Giải thích) Tạo hash ổn định từ bộ lọc + version token liên quan */
public final class FilterHash {
    public static String norm(String str) {
        return (str == null ? "" : str.trim().toLowerCase());
    }
    public static String of(ProductFilter filter, String catVer) {
        // Chuỗi băm gồm: catId|keyword|sort|dir|cv=categoryVersion
        String str = (filter.categoryId()==null ? "*" : filter.categoryId()) + "|" +
                norm(filter.keyword()) + "|" + norm(filter.sort()) + "|" + norm(filter.dir())
                + (filter.size() == null ? "10" : filter.size()) + "|"
                + (filter.minReviewCount() == null ? "0" : filter.minReviewCount()) + "|"
                + "|cv=" + (catVer==null? "0" : catVer);
        return DigestUtils.md5DigestAsHex(str.getBytes(StandardCharsets.UTF_8));
    }
    private FilterHash(){}
}
