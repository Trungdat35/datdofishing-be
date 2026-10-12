package com.datdofishing.catalog.service;

import com.datdofishing.catalog.domain.Brand;
import com.datdofishing.catalog.domain.Category;
import com.datdofishing.catalog.repo.BrandRepository;
import com.datdofishing.catalog.repo.CategoryRepository;
import com.datdofishing.catalog.repo.ProductRepository;
import com.datdofishing.catalog.web.dto.CatalogDtos.BrandRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.BrandResponse;
import com.datdofishing.catalog.web.dto.CatalogDtos.Breadcrumb;
import com.datdofishing.catalog.web.dto.CatalogDtos.CategoryNode;
import com.datdofishing.catalog.web.dto.CatalogDtos.CategoryRequest;
import com.datdofishing.common.error.BusinessException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, BrandRepository brands, ProductRepository products) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
    }

    /** Cây danh mục cho menu trang chủ (chỉ danh mục đang bật). Được cache Redis. */
    @Cacheable("categoryTree")
    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        return buildTree(categories.findAllByOrderBySortOrderAscIdAsc().stream().filter(Category::isActive).toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryNode> adminList() {
        return buildTree(categories.findAllByOrderBySortOrderAscIdAsc());
    }

    /** id của danh mục và toàn bộ danh mục con cháu, dùng để lọc sản phẩm. */
    @Transactional(readOnly = true)
    public Set<Long> idWithDescendants(String slug) {
        Category root = categories.findBySlug(slug).orElseThrow(() -> BusinessException.notFound("Danh mục"));
        List<Category> all = categories.findAll();
        Set<Long> result = new LinkedHashSet<>();
        result.add(root.getId());
        boolean added = true;
        while (added) {
            added = false;
            for (Category c : all) {
                if (c.getParentId() != null && result.contains(c.getParentId()) && result.add(c.getId())) {
                    added = true;
                }
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Breadcrumb> path(Long categoryId) {
        List<Breadcrumb> path = new ArrayList<>();
        Map<Long, Category> byId = new HashMap<>();
        categories.findAll().forEach(c -> byId.put(c.getId(), c));
        Category c = categoryId == null ? null : byId.get(categoryId);
        int guard = 0;
        while (c != null && guard++ < 10) {
            path.add(0, new Breadcrumb(c.getId(), c.getName(), c.getSlug()));
            c = c.getParentId() == null ? null : byId.get(c.getParentId());
        }
        return path;
    }

    @CacheEvict(value = {"categoryTree", "productDetail"}, allEntries = true)
    @Transactional
    public CategoryNode create(CategoryRequest req) {
        Category c = new Category();
        apply(c, req);
        categories.save(c);
        return toNode(c, List.of());
    }

    @CacheEvict(value = {"categoryTree", "productDetail"}, allEntries = true)
    @Transactional
    public CategoryNode update(Long id, CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> BusinessException.notFound("Danh mục"));
        if (Objects.equals(req.parentId(), id)) {
            throw BusinessException.badRequest("INVALID_PARENT", "Danh mục cha không thể là chính nó");
        }
        apply(c, req);
        return toNode(c, List.of());
    }

    @CacheEvict(value = {"categoryTree", "productDetail"}, allEntries = true)
    @Transactional
    public void delete(Long id) {
        if (products.countByCategoryId(id) > 0) {
            throw BusinessException.conflict("CATEGORY_IN_USE", "Danh mục còn sản phẩm, hãy chuyển sản phẩm trước");
        }
        categories.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> brands() {
        return brands.findAllByOrderByNameAsc().stream().map(CategoryService::toBrand).toList();
    }

    @CacheEvict(value = "productDetail", allEntries = true)
    @Transactional
    public BrandResponse saveBrand(Long id, BrandRequest req) {
        Brand b = id == null ? new Brand()
                : brands.findById(id).orElseThrow(() -> BusinessException.notFound("Thương hiệu"));
        b.setName(req.name().trim());
        String slug = StringUtils.hasText(req.slug()) ? Slugs.of(req.slug()) : Slugs.of(req.name());
        if (!slug.equals(b.getSlug()) && brands.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_EXISTS", "Slug thương hiệu đã tồn tại");
        }
        b.setSlug(slug);
        b.setLogoUrl(StringUtils.hasText(req.logoUrl()) ? ImageUrlValidator.validate(req.logoUrl()) : null);
        return toBrand(brands.save(b));
    }

    @Transactional
    public void deleteBrand(Long id) {
        brands.deleteById(id);
    }

    public static BrandResponse toBrand(Brand b) {
        return b == null ? null : new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl());
    }

    private void apply(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        String slug = StringUtils.hasText(req.slug()) ? Slugs.of(req.slug()) : Slugs.of(req.name());
        if (!slug.equals(c.getSlug()) && categories.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_EXISTS", "Slug danh mục đã tồn tại");
        }
        c.setSlug(slug);
        if (req.parentId() != null && !categories.existsById(req.parentId())) {
            throw BusinessException.notFound("Danh mục cha");
        }
        c.setParentId(req.parentId());
        c.setSortOrder(req.sortOrder());
        c.setImageUrl(StringUtils.hasText(req.imageUrl()) ? ImageUrlValidator.validate(req.imageUrl()) : null);
        c.setActive(req.active() == null || req.active());
    }

    private static List<CategoryNode> buildTree(List<Category> list) {
        Map<Long, List<Category>> byParent = new HashMap<>();
        for (Category c : list) {
            byParent.computeIfAbsent(c.getParentId() == null ? 0L : c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        return children(0L, byParent, 0);
    }

    private static List<CategoryNode> children(Long parentId, Map<Long, List<Category>> byParent, int depth) {
        if (depth > 5) {
            return List.of();
        }
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(c -> toNode(c, children(c.getId(), byParent, depth + 1))).toList();
    }

    private static CategoryNode toNode(Category c, List<CategoryNode> children) {
        return new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getParentId(), c.getSortOrder(),
                c.getImageUrl(), c.isActive(), children);
    }
}
