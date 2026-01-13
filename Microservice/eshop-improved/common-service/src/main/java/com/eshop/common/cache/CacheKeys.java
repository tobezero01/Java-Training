package com.eshop.common.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.stream.Stream;

/**
 * Cache Key Builder
 * 
 * Naming convention: {namespace}:{version}:{domain}:{entity}:{identifier}
 * Example: eshop:v1:catalog:product:123
 */
public final class CacheKeys {
    
    private static final String NAMESPACE = "eshop";
    private static final String VERSION = "v1";
    
    private CacheKeys() {}
    
    // ==================== KEY BUILDERS ====================
    
    private static String key(String... parts) {
        return String.join(":", Stream.concat(
            Stream.of(NAMESPACE, VERSION),
            Arrays.stream(parts)
        ).toList());
    }
    
    private static String safe(String value) {
        if (value == null) return "";
        // Replace special characters that might interfere with Redis
        return value.trim().toLowerCase().replaceAll("[:\\s]", "_");
    }
    
    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 8); // First 8 chars
        } catch (Exception e) {
            return String.valueOf(value.hashCode());
        }
    }

    // ==================== CATALOG KEYS ====================
    
    public static String productById(Integer id) {
        return key("catalog", "product", "id", String.valueOf(id));
    }
    
    public static String productByAlias(String alias) {
        return key("catalog", "product", "alias", safe(alias));
    }
    
    public static String categoryTree() {
        return key("catalog", "category", "tree");
    }
    
    public static String categoryTopLevel() {
        return key("catalog", "category", "top");
    }
    
    public static String categoryLeaves() {
        return key("catalog", "category", "leaves");
    }
    
    public static String categoryByAlias(String alias) {
        return key("catalog", "category", "alias", safe(alias));
    }
    
    public static String categoryPath(Integer categoryId) {
        return key("catalog", "category", "path", String.valueOf(categoryId));
    }
    
    /**
     * Product list page key with filter hash
     * filterHash includes: categoryId, keyword, sort, direction, categoryVersion
     */
    public static String productListPage(String filterHash, int page) {
        return key("catalog", "product", "list", filterHash, "p" + page);
    }
    
    /**
     * Category version counter for cache invalidation
     */
    public static String categoryVersion(Integer categoryId) {
        return key("catalog", "category", "version", String.valueOf(categoryId));
    }
    
    // ==================== CART KEYS ====================
    
    public static String cartItems(Integer customerId) {
        return key("cart", "items", String.valueOf(customerId));
    }
    
    public static String cartCount(Integer customerId) {
        return key("cart", "count", String.valueOf(customerId));
    }
    
    // ==================== CUSTOMER KEYS ====================
    
    public static String addressDefault(Integer customerId) {
        return key("customer", "address", "default", String.valueOf(customerId));
    }
    
    public static String addressById(Integer customerId, Integer addressId) {
        return key("customer", "address", String.valueOf(customerId), String.valueOf(addressId));
    }
    
    public static String addressList(Integer customerId) {
        return key("customer", "address", "list", String.valueOf(customerId));
    }
    
    // ==================== SHIPPING KEYS ====================
    
    public static String shippingRate(Integer countryId, String state) {
        return key("shipping", "rate", String.valueOf(countryId), safe(state));
    }
    
    // ==================== GEO KEYS ====================
    
    public static String geoCountries() {
        return key("geo", "countries");
    }
    
    public static String geoStates(Integer countryId) {
        return key("geo", "states", String.valueOf(countryId));
    }
    
    // ==================== SETTINGS KEYS ====================
    
    public static String settingsEmail() {
        return key("settings", "email");
    }
    
    public static String settingsPaypal() {
        return key("settings", "paypal");
    }
    
    public static String settingsCurrency() {
        return key("settings", "currency");
    }
    
    // ==================== SESSION/AUTH KEYS ====================
    
    public static String tokenBlacklist(String jti) {
        return key("auth", "blacklist", jti);
    }
    
    public static String userSession(Integer userId, String sessionId) {
        return key("auth", "session", String.valueOf(userId), sessionId);
    }
    
    // ==================== RATE LIMITING KEYS ====================
    
    public static String rateLimit(String identifier, String window) {
        return key("ratelimit", identifier, window);
    }
    
    // ==================== LOCK KEYS ====================
    
    public static String lock(String resource) {
        return key("lock", resource);
    }
    
    // ==================== SEARCH KEYS ====================
    
    public static String searchProducts(String keyword, int page) {
        return key("search", "products", hash(safe(keyword)), "p" + page);
    }
}

/**
 * Cache TTL Configuration
 */
final class CacheTTL {
    
    private CacheTTL() {}
    
    // Long-lived (rarely changes)
    public static final Duration GEO_DATA = Duration.ofDays(7);
    public static final Duration SETTINGS = Duration.ofHours(6);
    public static final Duration CATEGORY_TREE = Duration.ofHours(6);
    
    // Medium-lived
    public static final Duration PRODUCT_DETAIL = Duration.ofMinutes(15);
    public static final Duration SHIPPING_RATE = Duration.ofDays(1);
    public static final Duration ADDRESS = Duration.ofMinutes(30);
    
    // Short-lived (frequently changes)
    public static final Duration CART = Duration.ofMinutes(30);
    public static final Duration SEARCH_RESULTS = Duration.ofMinutes(5);
    public static final Duration PRODUCT_LIST_PAGE = Duration.ofMinutes(10);
    
    // Very short-lived
    public static final Duration NULL_MARKER = Duration.ofSeconds(60);
    public static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);
    
    // Auth related
    public static final Duration TOKEN_BLACKLIST = Duration.ofHours(24); // Should match JWT expiry
    public static final Duration SESSION = Duration.ofDays(7);
}
