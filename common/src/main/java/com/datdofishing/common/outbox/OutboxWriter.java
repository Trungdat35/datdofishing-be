package com.datdofishing.common.outbox;

import com.datdofishing.common.event.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Ghi sự kiện vào bảng outbox trong CÙNG transaction với dữ liệu nghiệp vụ.
 * OutboxRelay sẽ đọc và đẩy lên Kafka sau, nên không bao giờ mất sự kiện.
 * Cần bảng: outbox_events(id uuid, topic, msg_key, payload json, created_at, published_at).
 */
@Component
@ConditionalOnProperty(name = "datdofishing.outbox.enabled", havingValue = "true")
public class OutboxWriter {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OutboxWriter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void write(String topic, String key, String type, Object payload) {
        UUID id = UUID.randomUUID();
        EventEnvelope envelope = new EventEnvelope(id.toString(), type, Instant.now(),
                objectMapper.valueToTree(payload));
        try {
            String json = objectMapper.writeValueAsString(envelope);
            jdbc.update("INSERT INTO outbox_events(id, topic, msg_key, payload, created_at) "
                    + "VALUES (?, ?, ?, CAST(? AS JSON), NOW(6))", id.toString(), topic, key, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được sự kiện outbox", e);
        }
    }
}
