package com.datdofishing.promotion.web;

import com.datdofishing.promotion.service.FlashSaleService;
import com.datdofishing.promotion.service.PromotionDtos.ClaimRequest;
import com.datdofishing.promotion.service.PromotionDtos.ClaimedItem;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleRequest;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PromotionController {

    private final FlashSaleService flashSales;

    public PromotionController(FlashSaleService flashSales) {
        this.flashSales = flashSales;
    }

    /** Công khai: flash sale cho trang chủ (đồng hồ đếm ngược + số suất còn lại). 204 nếu không có. */
    @GetMapping("/api/promotions/flash-sales/current")
    public ResponseEntity<FlashSaleResponse> current() {
        FlashSaleResponse r = flashSales.current();
        return r == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(r);
    }

    // ----- Admin -----
    @GetMapping("/api/admin/promotions/flash-sales")
    public List<FlashSaleResponse> all() {
        return flashSales.all();
    }

    @GetMapping("/api/admin/promotions/flash-sales/{id}")
    public FlashSaleResponse get(@PathVariable Long id) {
        return flashSales.get(id);
    }

    @PostMapping("/api/admin/promotions/flash-sales")
    @ResponseStatus(HttpStatus.CREATED)
    public FlashSaleResponse create(@Valid @RequestBody FlashSaleRequest req) {
        return flashSales.save(null, req);
    }

    @PutMapping("/api/admin/promotions/flash-sales/{id}")
    public FlashSaleResponse update(@PathVariable Long id, @Valid @RequestBody FlashSaleRequest req) {
        return flashSales.save(id, req);
    }

    @DeleteMapping("/api/admin/promotions/flash-sales/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        flashSales.deactivate(id);
    }

    // ----- Nội bộ (order-service gọi, gateway không định tuyến) -----
    @PostMapping("/internal/flash-sales/claim")
    public List<ClaimedItem> claim(@RequestBody ClaimRequest req) {
        return flashSales.claim(req);
    }

    @PostMapping("/internal/flash-sales/release/{orderCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable String orderCode) {
        flashSales.release(orderCode);
    }
}
