package com.datdofishing.inventory.messaging;

import com.datdofishing.common.event.EventEnvelope;
import com.datdofishing.common.event.EventParser;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.ProcessedEventStore;
import com.datdofishing.common.event.Topics;
import com.datdofishing.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Nghe sự kiện từ catalog-service và order-service. Mỗi sự kiện chỉ xử lý một lần. */
@Component
public class InventoryListener {

    private final EventParser parser;
    private final ProcessedEventStore processed;
    private final InventoryService inventory;

    public InventoryListener(EventParser parser, ProcessedEventStore processed, InventoryService inventory) {
        this.parser = parser;
        this.processed = processed;
        this.inventory = inventory;
    }

    @KafkaListener(topics = Topics.PRODUCT_VARIANT_CREATED)
    @Transactional
    public void onVariantCreated(String message) {
        EventEnvelope e = parser.parse(message);
        if (processed.markProcessed(e.eventId())) {
            inventory.createStockIfMissing(parser.payload(e, Payloads.VariantCreated.class));
        }
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    @Transactional
    public void onOrderCreated(String message) {
        EventEnvelope e = parser.parse(message);
        if (processed.markProcessed(e.eventId())) {
            inventory.reserve(parser.payload(e, Payloads.OrderCreated.class));
        }
    }

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED)
    @Transactional
    public void onOrderStatusChanged(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.OrderStatusChanged p = parser.payload(e, Payloads.OrderStatusChanged.class);
        switch (p.status()) {
            case "CANCELLED" -> inventory.release(p.orderId());
            case "DELIVERED" -> inventory.commit(p.orderId());
            default -> {
                // Các trạng thái khác không ảnh hưởng tồn kho
            }
        }
    }
}
