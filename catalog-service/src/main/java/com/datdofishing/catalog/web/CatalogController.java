package com.datdofishing.catalog.web;

import com.datdofishing.catalog.service.CategoryService;
import com.datdofishing.catalog.service.ProductService;
import com.datdofishing.catalog.service.ProductService.ProductFilter;
import com.datdofishing.catalog.web.dto.CatalogDtos.BrandResponse;
import com.datdofishing.catalog.web.dto.CatalogDtos.CategoryNode;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductDetail;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductSummary;
import com.datdofishing.catalog.web.dto.CatalogDtos.VariantInfo;
import com.datdofishing.common.web.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API công khai cho site cửa hàng, không cần đăng nhập. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CategoryService categories;
    private final ProductService products;

    public CatalogController(CategoryService categories, ProductService products) {
        this.categories = categories;
        this.products = products;
    }

    @GetMapping("/categories")
    public List<CategoryNode> categories() {
        return categories.tree();
    }

    @GetMapping("/brands")
    public List<BrandResponse> brands() {
        return categories.brands();
    }

    /**
     * Ví dụ: /api/catalog/products?category=can-cau-dai&sort=price_asc&page=0&size=20
     * sort: newest | price_asc | price_desc | name
     */
    @GetMapping("/products")
    public PageResponse<ProductSummary> products(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String brand,
                                                 @RequestParam(required = false) Long minPrice,
                                                 @RequestParam(required = false) Long maxPrice,
                                                 @RequestParam(required = false) Boolean hot,
                                                 @RequestParam(name = "new", required = false) Boolean newArrival,
                                                 @RequestParam(defaultValue = "newest") String sort,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return products.search(new ProductFilter(q, category, brand, minPrice, maxPrice, hot, newArrival, null, sort,
                page, size), false);
    }

    @GetMapping("/products/{slug}")
    public ProductDetail product(@PathVariable String slug) {
        return products.detailBySlug(slug);
    }

    /** Thông tin nhiều biến thể cùng lúc, ví dụ /api/catalog/variants?ids=1,2,3 */
    @GetMapping("/variants")
    public List<VariantInfo> variants(@RequestParam List<Long> ids) {
        return products.variantInfos(ids);
    }
}
