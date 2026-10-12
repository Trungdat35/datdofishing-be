package com.datdofishing.promotion.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "flash_sale_items")
@Getter
@Setter
public class FlashSaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flash_sale_id")
    private FlashSale flashSale;
    private Long productId;
    private Long variantId;
    private String productName;
    private String productSlug;
    private String variantName;
    private String imageUrl;
    private long originalPrice;
    private long salePrice;
    private int quota;
    /** Đã bán + đang giữ cho đơn chưa hủy. */
    private int sold;
    private int sortOrder;
}
