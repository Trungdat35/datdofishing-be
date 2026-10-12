package com.datdofishing.catalog.web;

import com.datdofishing.catalog.service.CategoryService;
import com.datdofishing.catalog.service.ProductService;
import com.datdofishing.catalog.service.ProductService.ProductFilter;
import com.datdofishing.catalog.web.dto.CatalogDtos.BrandRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.BrandResponse;
import com.datdofishing.catalog.web.dto.CatalogDtos.CategoryNode;
import com.datdofishing.catalog.web.dto.CatalogDtos.CategoryRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductDetail;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductRequest;
import com.datdofishing.catalog.web.dto.CatalogDtos.ProductSummary;
import com.datdofishing.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Quản lý danh mục, thương hiệu, sản phẩm (kèm ảnh URL) cho site admin. Cần quyền ADMIN. */
@RestController
@RequestMapping("/api/admin/catalog")
public class AdminCatalogController {

    private final CategoryService categories;
    private final ProductService products;

    public AdminCatalogController(CategoryService categories, ProductService products) {
        this.categories = categories;
        this.products = products;
    }

    @GetMapping("/categories")
    public List<CategoryNode> categories() {
        return categories.adminList();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryNode createCategory(@Valid @RequestBody CategoryRequest req) {
        return categories.create(req);
    }

    @PutMapping("/categories/{id}")
    public CategoryNode updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return categories.update(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categories.delete(id);
    }

    @GetMapping("/brands")
    public List<BrandResponse> brands() {
        return categories.brands();
    }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public BrandResponse createBrand(@Valid @RequestBody BrandRequest req) {
        return categories.saveBrand(null, req);
    }

    @PutMapping("/brands/{id}")
    public BrandResponse updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest req) {
        return categories.saveBrand(id, req);
    }

    @DeleteMapping("/brands/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrand(@PathVariable Long id) {
        categories.deleteBrand(id);
    }

    @GetMapping("/products")
    public PageResponse<ProductSummary> products(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "newest") String sort,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return products.search(new ProductFilter(q, category, null, null, null, null, null, status, sort, page, size),
                true);
    }

    @GetMapping("/products/{id}")
    public ProductDetail product(@PathVariable Long id) {
        return products.detailById(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetail create(@Valid @RequestBody ProductRequest req) {
        return products.create(req);
    }

    @PutMapping("/products/{id}")
    public ProductDetail update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return products.update(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(@PathVariable Long id) {
        products.hide(id);
    }
}
