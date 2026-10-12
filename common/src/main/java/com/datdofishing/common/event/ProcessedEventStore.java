package com.datdofishing.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chống xử lý trùng sự kiện (consumer idempotent).
 * Cần bảng: processed_events(event_id varchar primary key, processed_at timestamptz).
 */
@Component
@ConditionalOnProperty(name = "datdofishing.outbox.enabled", havingValue = "true")
public class ProcessedEventStore {

    private final JdbcTemplate jdbc;

    public ProcessedEventStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Trả về true nếu sự kiện chưa từng xử lý (và đánh dấu đã xử lý trong cùng transaction). */
    public boolean markProcessed(String eventId) {
        int rows = jdbc.update(
                "INSERT IGNORE INTO processed_events(event_id, processed_at) VALUES (?, NOW(6))",
                eventId);
        return rows == 1;
    }
}
