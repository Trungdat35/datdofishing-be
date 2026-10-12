package com.datdofishing.cart.messaging;

import com.datdofishing.cart.service.CartService;
import com.datdofishing.common.event.EventEnvelope;
import com.datdofishing.common.event.EventParser;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Đặt hàng xong thì xóa các sản phẩm đã đặt khỏi giỏ (thao tác lặp lại nhiều lần vẫn an toàn). */
@Component
public class OrderCreatedListener {

    private final EventParser parser;
    private final CartService carts;

    public OrderCreatedListener(EventParser parser, CartService carts) {
        this.parser = parser;
        this.carts = carts;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(String message) {
        EventEnvelope e = parser.parse(message);
        Payloads.OrderCreated order = parser.payload(e, Payloads.OrderCreated.class);
        carts.removeVariants(order.userId(), order.items().stream().map(Payloads.OrderItem::variantId).toList());
    }
}
