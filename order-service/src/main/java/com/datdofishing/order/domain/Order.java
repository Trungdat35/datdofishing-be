package com.datdofishing.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    private Long userId;
    private String userEmail;
    private String userName;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private long subtotal;
    private long discount;
    private long shippingFee;
    private long total;
    private String paymentMethod;
    private String recipientName;
    private String phone;
    private String shippingAddress;
    private String note;
    private String cancelReason;
    private String createdByInstance;
    private Instant createdAt;
    private Instant updatedAt;

    /** Khóa lạc quan: tránh 2 instance cùng sửa một đơn. */
    @Version
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void addHistory(OrderStatus from, OrderStatus to, String note, String by) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(this);
        h.setFromStatus(from == null ? null : from.name());
        h.setToStatus(to.name());
        h.setNote(note);
        h.setChangedBy(by);
        h.setCreatedAt(Instant.now());
        history.add(h);
    }
}
