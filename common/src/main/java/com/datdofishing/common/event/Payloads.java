package com.datdofishing.common.event;

import java.util.List;

/** Dữ liệu (payload) của các sự kiện Kafka dùng chung giữa các service. */
public final class Payloads {

    private Payloads() {
    }

    public record UserRegistered(Long userId, String email, String fullName, String phone, String provider,
                                 String verifyToken) {
    }

    public record PasswordResetRequested(Long userId, String email, String fullName, String resetToken) {
    }

    public record VariantCreated(Long variantId, Long productId, String sku) {
    }

    public record OrderItem(Long variantId, Long productId, String productName, String variantName, int quantity,
                            long unitPrice) {
    }

    public record OrderCreated(Long orderId, String code, Long userId, String userEmail, String userName,
                               long total, List<OrderItem> items) {
    }

    public record OrderStatusChanged(Long orderId, String code, Long userId, String userEmail, String userName,
                                     String status, String previousStatus, long total, String reason,
                                     List<OrderItem> items) {
    }

    public record InventoryResult(Long orderId, String code, String reason) {
    }
}
