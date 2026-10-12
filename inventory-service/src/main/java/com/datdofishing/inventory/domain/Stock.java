package com.datdofishing.inventory.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "stock")
@Getter
@Setter
public class Stock {
    @Id
    private Long variantId;
    private Long productId;
    /** Số lượng thực có trong kho. */
    private int quantity;
    /** Số lượng đang giữ cho đơn chưa giao. */
    private int reserved;
    private int lowStockThreshold = 5;
    private Instant updatedAt;

    public int available() {
        return Math.max(quantity - reserved, 0);
    }

    public StockStatus status() {
        int a = available();
        if (a <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        return a <= lowStockThreshold ? StockStatus.LOW_STOCK : StockStatus.IN_STOCK;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
