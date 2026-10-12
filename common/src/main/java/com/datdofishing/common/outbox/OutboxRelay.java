package com.datdofishing.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Đọc outbox và đẩy lên Kafka. Dùng FOR UPDATE SKIP LOCKED nên khi chạy nhiều instance
 * (order-service x3) mỗi bản ghi chỉ do một instance gửi.
 */
@Component
@ConditionalOnProperty(name = "datdofishing.outbox.enabled", havingValue = "true")
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = tx;
    }

    @Scheduled(fixedDelayString = "${datdofishing.outbox.poll-ms:1000}")
    public void publish() {
        try {
            tx.executeWithoutResult(status -> {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT id, topic, msg_key, CAST(payload AS CHAR) AS payload FROM outbox_events "
                                + "WHERE published_at IS NULL ORDER BY created_at LIMIT 50 FOR UPDATE SKIP LOCKED");
                for (Map<String, Object> row : rows) {
                    String topic = (String) row.get("topic");
                    String key = (String) row.get("msg_key");
                    String payload = (String) row.get("payload");
                    try {
                        kafka.send(topic, key, payload).get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new IllegalStateException("Gửi Kafka thất bại, sẽ thử lại", e);
                    }
                    jdbc.update("UPDATE outbox_events SET published_at = NOW(6) WHERE id = ?", String.valueOf(row.get("id")));
                }
            });
        } catch (Exception e) {
            log.warn("Outbox chưa gửi được: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        jdbc.update("DELETE FROM outbox_events WHERE published_at < NOW(6) - INTERVAL 7 DAY");
    }
}
