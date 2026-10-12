package com.datdofishing.inventory.web;

import com.datdofishing.common.web.PageResponse;
import com.datdofishing.inventory.service.InventoryService;
import com.datdofishing.inventory.service.InventoryService.PublicStock;
import com.datdofishing.inventory.service.InventoryService.StockResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class InventoryController {

    private final InventoryService inventory;

    public InventoryController(InventoryService inventory) {
        this.inventory = inventory;
    }

    /** Công khai: /api/inventory/stock?variantIds=1,2,3 -> còn hàng / sắp hết / hết hàng. */
    @GetMapping("/api/inventory/stock")
    public List<PublicStock> stock(@RequestParam List<Long> variantIds) {
        return inventory.publicStock(variantIds);
    }

    @GetMapping("/internal/stock")
    public List<PublicStock> internalStock(@RequestParam List<Long> variantIds) {
        return inventory.publicStock(variantIds);
    }

    @GetMapping("/api/admin/inventory")
    public PageResponse<StockResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return inventory.list(page, size);
    }

    public record UpdateStockRequest(@Min(0) int quantity, Integer lowStockThreshold) {
    }

    @PutMapping("/api/admin/inventory/{variantId}")
    public StockResponse update(@PathVariable Long variantId, @Valid @RequestBody UpdateStockRequest req) {
        return inventory.update(variantId, req.quantity(), req.lowStockThreshold());
    }
}
