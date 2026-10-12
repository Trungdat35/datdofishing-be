package com.datdofishing.order.domain;

import java.util.Map;
import java.util.Set;

/**
 * PENDING: chờ kho giữ hàng -> CONFIRMED: đã giữ hàng -> SHIPPING: đang giao -> DELIVERED: đã giao.
 * CANCELLED: đã hủy (hết hàng, khách hủy, admin hủy hoặc quá hạn).
 */
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = Map.of(
            PENDING, Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(SHIPPING, CANCELLED),
            SHIPPING, Set.of(DELIVERED, CANCELLED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of());

    public boolean canMoveTo(OrderStatus target) {
        return NEXT.get(this).contains(target);
    }
}
