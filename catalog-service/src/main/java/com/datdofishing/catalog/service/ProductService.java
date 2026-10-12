package com.datdofishing.catalog.service;

import com.datdofishing.catalog.domain.Brand;
import com.datdofishing.catalog.domain.Product;
import com.datdofishing.catalog.domain.ProductImage;
import com.datdofishing.catalog.domain.ProductStatus;
import com.datdofishing.catalog.domain.ProductVariant;
import com.datdofishing.catalog.repo.BrandRepository;
import com.datdofishing.catalog.repo.CategoryRepository;
import com.datdofishing.catalog.repo.ProductRepository;
import com.datdofishing.catalog.repo.ProductVariantRepository;
import com.datdofishing.catalog.web.dto.CatalogDtos.ImageRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.ImageResponse;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductDetail;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductSummary;
import com.datdofishing.catalog.web.dto.CatalogDtos.VariantInfo;
import com.datdofishing.catalog.web.dto.CatalogDtos.VariantRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.VariantResponse;
import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.event.Topics;
import com.datdofishing.common.outbox.OutboxWriter;
import com.datdofishing.common.web.PageResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final CategoryService categoryService;
    private final OutboxWriter outbox;

    public ProductService(ProductRepository products, ProductVariantRepository variants, BrandRepository brands,
                          CategoryRepository categories, CategoryService categoryService, OutboxWriter outbox) {
        this.products = products;
        this.variants = variants;
        this.brands = brands;
        this.categories = categories;
        this.categoryService = categoryService;
        this.outbox = outbox;
    }

    /** Bộ lọc tìm kiếm sản phẩm. */
    public record ProductFilter(String q, String category, String brand, Long minPrice, Long maxPrice, Boolean hot,
                                Boolean newArrival, String status, String sort, int page, int size) {
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> search(ProductFilter f, boolean admin) {
        Set<Long> categoryIds = StringUtils.hasText(f.category()) ? categoryService.idWithDescendants(f.category()) : null;
        Long brandId = StringUtils.hasText(f.brand())
                ? brands.findBySlug(f.brand()).map(Brand::getId).orElse(-1L) : null;

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (admin) {
                if (StringUtils.hasText(f.status())) {
                    ps.add(cb.equal(root.get("status"), ProductStatus.valueOf(f.status().toUpperCase(Locale.ROOT))));
                }
            } else {
                ps.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
            }
            if (StringUtils.hasText(f.q())) {
                String like = "%" + f.q().trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
            }
            if (categoryIds != null) {
                ps.add(root.get("categoryId").in(categoryIds));
            }
            if (brandId != null) {
                ps.add(cb.equal(root.get("brandId"), brandId));
            }
            if (f.minPrice() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("price"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("price"), f.maxPrice()));
            }
            if (Boolean.TRUE.equals(f.hot())) {
                ps.add(cb.isTrue(root.get("hot")));
            }
            if (Boolean.TRUE.equals(f.newArrival())) {
                ps.add(cb.isTrue(root.get("newArrival")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Sort sort = switch (f.sort() == null ? "newest" : f.sort()) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "name" -> Sort.by("name").ascending();
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
        Page<Product> page = products.findAll(spec, PageRequest.of(Math.max(f.page(), 0),
                Math.min(Math.max(f.size(), 1), 100), sort));
        Map<Long, String> brandNames = brandNames(page.getContent().stream().map(Product::getBrandId).toList());
        return PageResponse.of(page, p -> toSummary(p, brandNames.get(p.getBrandId())));
    }

    @Cacheable(value = "productDetail", key = "#slug")
    @Transactional(readOnly = true)
    public ProductDetail detailBySlug(String slug) {
        Product p = products.findBySlug(slug)
                .filter(x -> x.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm"));
        return toDetail(p, false);
    }

    @Transactional(readOnly = true)
    public ProductDetail detailById(Long id) {
        return toDetail(get(id), true);
    }

    /** Thông tin biến thể cho giỏ hàng / đặt hàng. */
    @Transactional(readOnly = true)
    public List<VariantInfo> variantInfos(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return variants.findWithProductByIdIn(ids).stream().map(v -> {
            Product p = v.getProduct();
            return new VariantInfo(v.getId(), p.getId(), p.getName(), p.getSlug(), v.getName(), v.getSku(),
                    v.getPrice(), v.getCompareAtPrice(), p.thumbnailUrl(),
                    v.isActive() && p.getStatus() == ProductStatus.ACTIVE);
        }).toList();
    }

    @CacheEvict(value = "productDetail", allEntries = true)
    @Transactional
    public ProductDetail create(ProductRequest req) {
        Product p = new Product();
        apply(p, req);
        products.save(p);
        publishNewVariants(p, List.of());
        return toDetail(p, true);
    }

    @CacheEvict(value = "productDetail", allEntries = true)
    @Transactional
    public ProductDetail update(Long id, ProductRequest req) {
        Product p = get(id);
        List<Long> existingIds = p.getVariants().stream().map(ProductVariant::getId).toList();
        apply(p, req);
        products.flush();
        publishNewVariants(p, existingIds);
        return toDetail(p, true);
    }

    /** Không xóa hẳn để giữ lịch sử đơn hàng, chỉ ẩn sản phẩm. */
    @CacheEvict(value = "productDetail", allEntries = true)
    @Transactional
    public void hide(Long id) {
        Product p = get(id);
        p.setStatus(ProductStatus.HIDDEN);
    }

    private void apply(Product p, ProductRequest req) {
        p.setName(req.name().trim());
        String slug = StringUtils.hasText(req.slug()) ? Slugs.of(req.slug()) : Slugs.of(req.name());
        if (!slug.equals(p.getSlug()) && products.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_EXISTS", "Slug sản phẩm đã tồn tại: " + slug);
        }
        p.setSlug(slug);
        p.setSku(req.sku());
        if (req.categoryId() != null && !categories.existsById(req.categoryId())) {
            throw BusinessException.notFound("Danh mục");
        }
        if (req.brandId() != null && !brands.existsById(req.brandId())) {
            throw BusinessException.notFound("Thương hiệu");
        }
        p.setCategoryId(req.categoryId());
        p.setBrandId(req.brandId());
        p.setShortDescription(req.shortDescription());
        p.setDescription(req.description());
        p.setStatus(StringUtils.hasText(req.status())
                ? ProductStatus.valueOf(req.status().toUpperCase(Locale.ROOT)) : ProductStatus.ACTIVE);
        p.setHot(req.hot());
        p.setNewArrival(req.newArrival());
        p.setSpecs(req.specs());
        syncVariants(p, req.variants());
        syncImages(p, req.images() == null ? List.of() : req.images());

        ProductVariant cheapest = p.getVariants().stream().filter(ProductVariant::isActive)
                .min((a, b) -> Long.compare(a.getPrice(), b.getPrice()))
                .orElse(p.getVariants().get(0));
        p.setPrice(cheapest.getPrice());
        p.setCompareAtPrice(cheapest.getCompareAtPrice());
    }

    /** Cập nhật biến thể theo id; biến thể bị bỏ khỏi danh sách sẽ ngừng bán (không xóa). */
    private void syncVariants(Product p, List<VariantRequest> reqs) {
        Map<Long, ProductVariant> current = new HashMap<>();
        p.getVariants().forEach(v -> current.put(v.getId(), v));
        List<Long> keep = new ArrayList<>();
        for (VariantRequest r : reqs) {
            ProductVariant v = r.id() == null ? null : current.get(r.id());
            if (v == null) {
                if (variants.existsBySku(r.sku().trim())) {
                    throw BusinessException.conflict("SKU_EXISTS", "SKU đã tồn tại: " + r.sku());
                }
                v = new ProductVariant();
                v.setProduct(p);
                p.getVariants().add(v);
            } else {
                keep.add(v.getId());
            }
            v.setSku(r.sku().trim());
            v.setName(r.name().trim());
            v.setPrice(r.price());
            v.setCompareAtPrice(r.compareAtPrice());
            v.setActive(r.active() == null || r.active());
            v.setSortOrder(r.sortOrder());
        }
        for (ProductVariant v : current.values()) {
            if (!keep.contains(v.getId())) {
                v.setActive(false);
            }
        }
    }

    private void syncImages(Product p, List<ImageRequest> reqs) {
        p.getImages().clear();
        boolean hasThumb = reqs.stream().anyMatch(ImageRequest::thumbnail);
        int i = 0;
        for (ImageRequest r : reqs) {
            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setUrl(ImageUrlValidator.validate(r.url()));
            img.setAlt(StringUtils.hasText(r.alt()) ? r.alt() : p.getName());
            img.setSortOrder(r.sortOrder() == 0 ? i + 1 : r.sortOrder());
            img.setThumbnail(r.thumbnail() || (!hasThumb && i == 0));
            p.getImages().add(img);
            i++;
        }
    }

    /** Biến thể mới -> báo inventory-service tạo dòng tồn kho. */
    private void publishNewVariants(Product p, List<Long> existingIds) {
        for (ProductVariant v : p.getVariants()) {
            if (v.getId() != null && !existingIds.contains(v.getId())) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("variantId", v.getId());
                payload.put("productId", p.getId());
                payload.put("sku", v.getSku());
                outbox.write(Topics.PRODUCT_VARIANT_CREATED, String.valueOf(v.getId()), "VariantCreated", payload);
            }
        }
    }

    private Product get(Long id) {
        return products.findById(id).orElseThrow(() -> BusinessException.notFound("Sản phẩm"));
    }

    private Map<Long, String> brandNames(List<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        brands.findAllById(ids.stream().filter(Objects::nonNull).distinct().toList())
                .forEach(b -> map.put(b.getId(), b.getName()));
        return map;
    }

    static int discount(Long price, Long compareAt) {
        if (compareAt == null || compareAt <= 0 || price == null || compareAt <= price) {
            return 0;
        }
        return (int) Math.round((compareAt - price) * 100.0 / compareAt);
    }

    private ProductSummary toSummary(Product p, String brandName) {
        return new ProductSummary(p.getId(), p.getName(), p.getSlug(), p.getPrice(), p.getCompareAtPrice(),
                discount(p.getPrice(), p.getCompareAtPrice()), p.thumbnailUrl(), p.isHot(), p.isNewArrival(),
                p.getStatus().name(), brandName, p.getCategoryId());
    }

    private ProductDetail toDetail(Product p, boolean includeInactiveVariants) {
        Brand brand = p.getBrandId() == null ? null : brands.findById(p.getBrandId()).orElse(null);
        List<VariantResponse> vs = p.getVariants().stream()
                .filter(v -> includeInactiveVariants || v.isActive())
                .map(v -> new VariantResponse(v.getId(), v.getSku(), v.getName(), v.getPrice(), v.getCompareAtPrice(),
                        v.isActive(), v.getSortOrder()))
                .toList();
        List<ImageResponse> imgs = p.getImages().stream()
                .map(i -> new ImageResponse(i.getId(), i.getUrl(), i.getAlt(), i.getSortOrder(), i.isThumbnail()))
                .toList();
        Map<String, Object> specs = p.getSpecs() == null ? null : new LinkedHashMap<>(p.getSpecs());
        return new ProductDetail(p.getId(), p.getName(), p.getSlug(), p.getSku(), p.getShortDescription(),
                p.getDescription(), p.getPrice(), p.getCompareAtPrice(), discount(p.getPrice(), p.getCompareAtPrice()),
                p.getStatus().name(), p.isHot(), p.isNewArrival(), specs, CategoryService.toBrand(brand),
                categoryService.path(p.getCategoryId()), new ArrayList<>(vs), new ArrayList<>(imgs),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
