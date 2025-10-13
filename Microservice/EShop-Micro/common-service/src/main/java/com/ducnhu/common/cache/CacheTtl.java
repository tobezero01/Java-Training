package com.ducnhu.common.cache;

import java.time.Duration;

public final class CacheTtl {
    public static final Duration CATEGORY = Duration.ofHours(6);   // category thay đổi ít
    public static final Duration PRODUCT = Duration.ofMinutes(10);// product detail
    public static final Duration SEARCH = Duration.ofMinutes(2); // search kết quả nhanh lỗi thời
    public static final Duration SHIPPING = Duration.ofDays(1);    // rate ít đổi
    public static final Duration SETTINGS = Duration.ofMinutes(30);// email/payment/currency
    public static final Duration GEO = Duration.ofDays(1);    // countries/states
    public static final Duration ADDRESS = Duration.ofMinutes(5);
    public static final Duration NULL_SHORT = Duration.ofSeconds(45);

    public static final Duration PAGE_INDEX = Duration.ofMinutes(10); // list ổn định (category)
    public static final Duration PAGE_INDEX_SEARCH = Duration.ofSeconds(2);
    public static final Duration ITEM_DETAIL = Duration.ofMinutes(10); // chi tiết item
    public static final Duration ALIAS_TO_ID = Duration.ofDays(1);

    private CacheTtl() {}
}
