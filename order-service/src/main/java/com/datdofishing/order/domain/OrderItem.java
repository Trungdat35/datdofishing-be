package com.datdofishing.order.domain;

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

/** Lưu lại tên, giá tại thời điểm đặt để đơn không đổi khi sản phẩm đổi giá. */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    private Long productId;
    private Long variantId;
    private String productName;
    private String variantName;
    private String sku;
    private String imageUrl;
    private long unitPrice;
    private long originalPrice;
    private boolean flashSale;
    private int quantity;
    private long lineTotal;
}
