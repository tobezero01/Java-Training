package com.ducnhu.common.cache;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

public final class CacheKey {
    private static final String NS = "eshop";
    private static final String V  = "v1";

    public static String ns(String... parts) {
        return String.join(":", Stream.concat(Stream.of(NS, V), Arrays.stream(parts)).toList());
    }
    private static String safe(String s) { return (s==null?"":s.trim().toLowerCase()); }


    // 3) Category
    public static String catTree()            { return ns("cat","tree"); }
    public static String catTop()             { return ns("cat","top"); }
    public static String catLeaves()          { return ns("cat","leaves"); }
    public static String catByAlias(String alias)         { return ns("cat","alias", alias==null?"":alias.trim().toLowerCase()); }
    public static String catPath(Integer id)              { return ns("cat","path", String.valueOf(id)); }

    // 4) Product
    public static String productByAlias(String alias) { return ns("prod","alias", alias.toLowerCase()); }
    public static String productSearch(String keyword, int page) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String hash = DigestUtils.md5DigestAsHex(kw.getBytes(StandardCharsets.UTF_8));
        return ns("prod","search", hash, String.valueOf(page));
    }
    public static String prodById(Integer id)             { return ns("prod","id", String.valueOf(id)); }
    public static String prodByCategory(Integer catId)    { return ns("prod","byCat", String.valueOf(catId)); }


    // 5) ShippingRate
    public static String shipRate(Integer countryId, String stateKey) {
        String s = (stateKey == null ? "" : stateKey.trim().toLowerCase());
        return ns("ship","rate", String.valueOf(countryId), s);
    }

    // 7) Geo
    public static String geoCountries()   { return ns("geo","countries"); }
    public static String geoStatesOf(Integer countryId) { return ns("geo","states", String.valueOf(countryId)); }

    public static String addrDefault(Integer customerId) { return ns("addr","default", String.valueOf(customerId)); }
    public static String addrById(Integer customerId, Integer addressId) { return ns("addr","byId", String.valueOf(customerId), String.valueOf(addressId)); }
    public static String reviewVotes(Integer customerId, String idsHash) { return ns("review","votes", String.valueOf(customerId), idsHash); }

    /** bộ lọc đã băm + số trang Ids + metadata */
    public static String prodList(String filterHash, int page) {
        return ns("prod", "list", filterHash, "p", String.valueOf(page));
    }
    // Invalidate rẻ: khi thay đổi Category, INCR key này → filterHash đổi → toàn bộ page list cũ tự vô hiệu
    public static String catVer(Integer catId) {
        return ns("cat","ver", String.valueOf(catId));
    }

    public static String prodAlias2Id(String alias) {     // alias2id => tra nhanh id, rồi đọc prod:id:{id}
        return ns("prod","alias2id", safe(alias));
    }
    public static String prodIdBitmap() {                 // 1 bit / id (id là offset)
        return ns("prod","exist","bitmap");
    }
    private CacheKey() {}
}
