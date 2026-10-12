package com.datdofishing.notification.sse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Giữ các kết nối SSE đang mở trên instance này.
 * Thông báo đi qua Redis pub/sub nên dù người dùng kết nối vào instance nào cũng nhận được.
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);
    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final ObjectMapper objectMapper;
    private final Map<Long, List<SseEmitter>> users = new ConcurrentHashMap<>();
    private final List<SseEmitter> admins = new CopyOnWriteArrayList<>();

    public SseHub(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribeUser(Long userId) {
        SseEmitter emitter = newEmitter();
        List<SseEmitter> list = users.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(e -> remove.run());
        hello(emitter);
        return emitter;
    }

    public SseEmitter subscribeAdmin() {
        SseEmitter emitter = newEmitter();
        admins.add(emitter);
        Runnable remove = () -> admins.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(() -> {
            remove.run();
            emitter.complete();
        });
        emitter.onError(e -> remove.run());
        hello(emitter);
        return emitter;
    }

    /** Nhận tin từ Redis channel và đẩy tới các kết nối phù hợp trên instance này. */
    public void onRedisMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String audience = node.path("audience").asText();
            String event = node.path("event").asText("notification");
            JsonNode payload = node.path("payload");
            if ("ADMIN".equals(audience)) {
                admins.forEach(e -> send(e, event, payload));
            } else {
                long userId = node.path("userId").asLong();
                users.getOrDefault(userId, List.of()).forEach(e -> send(e, event, payload));
            }
        } catch (IOException ex) {
            log.warn("Tin SSE không hợp lệ: {}", ex.getMessage());
        }
    }

    /** Gửi "ping" định kỳ để proxy/gateway không cắt kết nối rảnh. */
    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        users.values().forEach(list -> list.forEach(this::ping));
        admins.forEach(this::ping);
    }

    public int connectionCount() {
        return admins.size() + users.values().stream().mapToInt(List::size).sum();
    }

    private SseEmitter newEmitter() {
        return new SseEmitter(TIMEOUT_MS);
    }

    private void hello(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"ok\":true}", MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void ping(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
        }
    }

    private void send(SseEmitter emitter, String event, JsonNode payload) {
        try {
            emitter.send(SseEmitter.event().name(event).data(payload.toString(), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
        }
    }
}
