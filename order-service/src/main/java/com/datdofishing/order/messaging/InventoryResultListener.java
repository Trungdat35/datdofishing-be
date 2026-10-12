package com.datdofishing.order.messaging;

import com.datdofishing.common.event.EventEnvelope;
import com.datdofishing.common.event.EventParser;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.ProcessedEventStore;
import com.datdofishing.common.event.Topics;
import com.datdofishing.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bước tiếp theo của saga: nhận kết quả giữ hàng từ inventory-service.
 * 3 instance cùng consumer group nên mỗi sự kiện chỉ 1 instance nhận; processed_events chống xử lý lặp.
 */
@Component
public class InventoryResultListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryResultListener.class);

    private final EventParser parser;
    private final ProcessedEventStore processed;
    private final OrderService orders;
    private final String instanceId;

    public InventoryResultListener(EventParser parser, ProcessedEventStore processed, OrderService orders,
                                   @Value("${datdofishing.instance-id}") String instanceId) {
        this.parser = parser;
        this.processed = processed;
        this.orders = orders;
        this.instanceId = instanceId;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    @Transactional
    public void onReserved(String message) {
        EventEnvelope e = parser.parse(message);
        if (processed.markProcessed(e.eventId())) {
            Payloads.InventoryResult r = parser.payload(e, Payloads.InventoryResult.class);
            log.info("[{}] Nhận inventory.reserved cho đơn {}", instanceId, r.code());
            orders.onInventoryReserved(r.orderId());
        }
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED)
    @Transactional
    public void onRejected(String message) {
        EventEnvelope e = parser.parse(message);
        if (processed.markProcessed(e.eventId())) {
            Payloads.InventoryResult r = parser.payload(e, Payloads.InventoryResult.class);
            log.info("[{}] Nhận inventory.rejected cho đơn {}", instanceId, r.code());
            orders.onInventoryRejected(r.orderId(), r.reason());
        }
    }
}
