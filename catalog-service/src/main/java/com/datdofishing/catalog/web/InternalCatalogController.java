package com.datdofishing.catalog.web;

import com.datdofishing.catalog.service.ProductService;
import com.datdofishing.catalog.web.dto.CatalogDtos.VariantInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API nội bộ giữa các service. Gateway không định tuyến /internal/** nên bên ngoài không gọi được. */
@RestController
public class InternalCatalogController {

    private final ProductService products;

    public InternalCatalogController(ProductService products) {
        this.products = products;
    }

    @GetMapping("/internal/variants")
    public List<VariantInfo> variants(@RequestParam List<Long> ids) {
        return products.variantInfos(ids);
    }
}
