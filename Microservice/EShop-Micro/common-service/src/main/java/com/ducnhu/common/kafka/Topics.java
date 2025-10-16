package com.ducnhu.common.kafka;

public final class Topics {
    private Topics() {
    }

    // CART
    public static final String CART_GET_REQ = "eshop.cart.get.req";
    public static final String CART_GET_RESP = "eshop.cart.get.resp";
    public static final String CART_CLEAR_CMD = "eshop.cart.clear.cmd";

    // SHIPPING
    public static final String SHIP_RATE_REQ = "eshop.shipping.rate.req";
    public static final String SHIP_RATE_RESP = "eshop.shipping.rate.resp";

    // CATALOG
    public static final String CATALOG_PROD_SNAPSHOT_REQ = "eshop.catalog.product.snapshot.req";
    public static final String CATALOG_PROD_SNAPSHOT_RESP = "eshop.catalog.product.snapshot.resp";

    // CUSTOMER ADDRESS
    public static final String CUST_ADDR_REQ = "eshop.customer.address.req";
    public static final String CUST_ADDR_RESP = "eshop.customer.address.resp";

    //
    public static final String SETTINGS_EMAIL_REQ = "eshop.settings.email.req";
    public static final String SETTINGS_EMAIL_RESP = "eshop.settings.email.resp";
    public static final String SETTINGS_PAYPAL_REQ = "eshop.settings.paypal.req";
    public static final String SETTINGS_PAYPAL_RESP = "eshop.settings.paypal.resp";


    // ORDER EVENTS
    public static final String ORDER_EVENTS = "eshop.order.events";
    public static final String ORDER_PAID_EVENTS = "eshop.order.paid.events";    // OrderPaidEvent

    // ORDER QUERY (NEW - phục vụ review)
    public static final String ORDER_HAS_PURCHASED_REQ = "eshop.order.hasPurchased.req";
    public static final String ORDER_HAS_PURCHASED_RESP = "eshop.order.hasPurchased.resp";

    // REVIEW → CATALOG (update stats)
    public static final String REVIEW_STATS_CHANGED = "eshop.review.stats.changed";
}
