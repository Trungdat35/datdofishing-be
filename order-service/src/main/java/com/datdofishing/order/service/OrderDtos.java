package com.datdofishing.order.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ItemRequest(@NotNull Long variantId, @Min(1) @Max(99) int quantity) {
    }

    public record ShippingInfo(@NotBlank(message = "Vui lòng nhập tên người nhận") @Size(max = 150) String recipientName,
                               @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ")
                               String phone,
                               @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng") @Size(max = 500) String address) {
    }

    public record CreateOrderRequest(@NotEmpty(message = "Đơn hàng chưa có sản phẩm") @Valid List<ItemRequest> items,
                                     @NotNull @Valid ShippingInfo shipping,
                                     @Size(max = 500) String note,
                                     String paymentMethod) {
    }

    public record ItemResponse(Long productId, Long variantId, String productName, String variantName, String sku,
                               String imageUrl, long unitPrice, long originalPrice, boolean flashSale, int quantity,
                               long lineTotal) {
    }

    public record HistoryResponse(String fromStatus, String toStatus, String note, String changedBy,
                                  Instant createdAt) {
    }

    public record OrderResponse(Long id, String code, Long userId, String userEmail, String userName, String status,
                                long subtotal, long discount, long shippingFee, long total, String paymentMethod,
                                String recipientName, String phone, String shippingAddress, String note,
                                String cancelReason, String createdByInstance, Instant createdAt, Instant updatedAt,
                                List<ItemResponse> items, List<HistoryResponse> history) {
    }

    public record OrderSummary(Long id, String code, String status, long total, int itemCount, String firstItemName,
                               String firstItemImage, String recipientName, Instant createdAt) {
    }

    public record UpdateStatusRequest(@NotBlank String status, @Size(max = 500) String note) {
    }

    public record CancelRequest(@Size(max = 500) String reason) {
    }

    public record StatusStat(String status, long count, long total) {
    }
}
