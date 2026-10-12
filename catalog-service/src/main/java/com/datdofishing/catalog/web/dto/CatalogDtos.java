package com.datdofishing.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DTO của catalog. Các DTO trả về implements Serializable vì được cache trong Redis. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    // ---------- Trả về ----------
    public record CategoryNode(Long id, String name, String slug, Long parentId, int sortOrder, String imageUrl,
                               boolean active, List<CategoryNode> children) implements Serializable {
    }

    public record BrandResponse(Long id, String name, String slug, String logoUrl) implements Serializable {
    }

    public record ProductSummary(Long id, String name, String slug, Long price, Long compareAtPrice,
                                 int discountPercent, String thumbnailUrl, boolean hot, boolean newArrival,
                                 String status, String brandName, Long categoryId) implements Serializable {
    }

    public record VariantResponse(Long id, String sku, String name, Long price, Long compareAtPrice,
                                  boolean active, int sortOrder) implements Serializable {
    }

    public record ImageResponse(Long id, String url, String alt, int sortOrder, boolean thumbnail)
            implements Serializable {
    }

    public record Breadcrumb(Long id, String name, String slug) implements Serializable {
    }

    public record ProductDetail(Long id, String name, String slug, String sku, String shortDescription,
                                String description, Long price, Long compareAtPrice, int discountPercent,
                                String status, boolean hot, boolean newArrival, Map<String, Object> specs,
                                BrandResponse brand, List<Breadcrumb> categoryPath, List<VariantResponse> variants,
                                List<ImageResponse> images, Instant createdAt, Instant updatedAt)
            implements Serializable {
    }

    /** Dùng nội bộ cho cart-service và order-service. */
    public record VariantInfo(Long variantId, Long productId, String productName, String productSlug,
                              String variantName, String sku, Long price, Long compareAtPrice, String imageUrl,
                              boolean available) {
    }

    // ---------- Nhận vào (admin) ----------
    public record CategoryRequest(@NotBlank @Size(max = 150) String name, String slug, Long parentId,
                                  int sortOrder, @Size(max = 1000) String imageUrl, Boolean active) {
    }

    public record BrandRequest(@NotBlank @Size(max = 150) String name, String slug,
                               @Size(max = 1000) String logoUrl) {
    }

    public record VariantRequest(Long id, @NotBlank @Size(max = 80) String sku, @NotBlank @Size(max = 150) String name,
                                 @NotNull @Min(0) Long price, @Min(0) Long compareAtPrice, Boolean active,
                                 int sortOrder) {
    }

    /** Ảnh nhập bằng URL, ví dụ https://bizweb.dktcdn.net/thumb/medium/.../products/abc.jpg?v=123 */
    public record ImageRequest(@NotBlank @Size(max = 1000) String url, @Size(max = 255) String alt, int sortOrder,
                               boolean thumbnail) {
    }

    public record ProductRequest(@NotBlank @Size(max = 255) String name, String slug, @Size(max = 80) String sku,
                                 Long categoryId, Long brandId, @Size(max = 500) String shortDescription,
                                 String description, String status, boolean hot, boolean newArrival,
                                 Map<String, Object> specs,
                                 @NotNull @Size(min = 1, message = "Cần ít nhất 1 biến thể") @Valid
                                 List<VariantRequest> variants,
                                 @Valid List<ImageRequest> images) {
    }
}
