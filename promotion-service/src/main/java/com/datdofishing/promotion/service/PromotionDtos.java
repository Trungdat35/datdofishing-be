package com.datdofishing.promotion.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class PromotionDtos {

    private PromotionDtos() {
    }

    public record FlashSaleItemResponse(Long id, Long productId, Long variantId, String productName, String productSlug,
                                        String variantName, String imageUrl, long originalPrice, long salePrice,
                                        int discountPercent, int quota, int sold, int remaining, boolean soldOut) {
    }

    /** serverTime giúp frontend đếm ngược chính xác dù đồng hồ máy khách lệch. */
    public record FlashSaleResponse(Long id, String name, Instant startAt, Instant endAt, boolean active,
                                    boolean running, Instant serverTime, List<FlashSaleItemResponse> items) {
    }

    public record FlashSaleItemRequest(@NotNull Long variantId, @NotNull @Min(0) Long salePrice, @Min(0) int quota,
                                       int sortOrder) {
    }

    public record FlashSaleRequest(@NotBlank String name, @NotNull Instant startAt, @NotNull Instant endAt,
                                   Boolean active, @NotEmpty @Valid List<FlashSaleItemRequest> items) {
    }

    public record ClaimItem(Long variantId, int quantity) {
    }

    public record ClaimRequest(String orderCode, Long userId, List<ClaimItem> items) {
    }

    public record ClaimedItem(Long variantId, int quantity, long salePrice, Long flashSaleId) {
    }

    public record VariantInfo(Long variantId, Long productId, String productName, String productSlug,
                              String variantName, String sku, Long price, Long compareAtPrice, String imageUrl,
                              boolean available) {
    }
}
