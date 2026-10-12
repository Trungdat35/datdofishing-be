package com.datdofishing.cart.service;

/** Bản sao cấu trúc VariantInfo của catalog-service. */
public record VariantInfo(Long variantId, Long productId, String productName, String productSlug, String variantName,
                          String sku, Long price, Long compareAtPrice, String imageUrl, boolean available) {
}
