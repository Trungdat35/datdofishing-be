package com.datdofishing.common.event;

/** Tên các topic Kafka dùng trong hệ thống. */
public final class Topics {
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_PASSWORD_RESET = "user.password-reset-requested";
    public static final String PRODUCT_VARIANT_CREATED = "product.variant-created";
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_STATUS_CHANGED = "order.status-changed";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_REJECTED = "inventory.rejected";

    public static final String[] ALL = {USER_REGISTERED, USER_PASSWORD_RESET, PRODUCT_VARIANT_CREATED,
            ORDER_CREATED, ORDER_STATUS_CHANGED, INVENTORY_RESERVED, INVENTORY_REJECTED};

    private Topics() {
    }
}
