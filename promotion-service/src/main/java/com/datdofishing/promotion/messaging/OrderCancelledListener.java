package com.datdofishing.promotion.messaging;

import com.datdofishing.common.event.EventEnvelope;
import com.datdofishing.common.event.EventParser;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.ProcessedEventStore;
import com.datdofishing.common.event.Topics;
import com.datdofishing.promotion.service.FlashSaleService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Đơn bị hủy -> trả lại suất flash sale. */
@Component
public class OrderCancelledListener {

    private final EventParser parser;
    private final ProcessedEventStore processed;
    private final FlashSaleService flashSales;

    public OrderCancelledListener(EventParser parser, ProcessedEventStore processed, FlashSaleService flashSales) {
        this.parser = parser;
        this.processed = processed;
        this.flashSales = flashSales;
    }

    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED)
    @Transactional
    public void onStatusChanged(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.OrderStatusChanged p = parser.payload(e, Payloads.OrderStatusChanged.class);
        if ("CANCELLED".equals(p.status())) {
            flashSales.release(p.code());
        }
    }
}
