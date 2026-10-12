package com.datdofishing.notification.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.notification.config.AppConfig;
import com.datdofishing.notification.domain.Notification;
import com.datdofishing.notification.repo.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lưu thông báo vào DB và phát realtime qua Redis -> SSE. */
@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository repo, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.repo = repo;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public record NotificationDto(Long id, String audience, Long userId, String type, String title, String body,
                                  String link, Map<String, Object> data, boolean read, Instant createdAt) {
    }

    @Transactional
    public void toUser(Long userId, String type, String title, String body, String link, Map<String, Object> data) {
        save(Notification.USER, userId, type, title, body, link, data);
    }

    @Transactional
    public void toAdmins(String type, String title, String body, String link, Map<String, Object> data) {
        save(Notification.ADMIN, null, type, title, body, link, data);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> mine(Long userId, int page, int size) {
        return PageResponse.of(repo.findByAudienceAndUserIdOrderByCreatedAtDesc(Notification.USER, userId,
                PageRequest.of(page, Math.min(size, 50))), NotificationService::toDto);
    }

    @Transactional(readOnly = true)
    public long unread(Long userId) {
        return repo.countByAudienceAndUserIdAndReadFalse(Notification.USER, userId);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> admin(int page, int size) {
        return PageResponse.of(repo.findByAudienceOrderByCreatedAtDesc(Notification.ADMIN,
                PageRequest.of(page, Math.min(size, 50))), NotificationService::toDto);
    }

    @Transactional
    public void markRead(Long userId, Long id) {
        Notification n = repo.findByIdAndUserId(id, userId).orElseThrow(() -> BusinessException.notFound("Thông báo"));
        n.setRead(true);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repo.markAllRead(userId);
    }

    private void save(String audience, Long userId, String type, String title, String body, String link,
                      Map<String, Object> data) {
        Notification n = new Notification();
        n.setAudience(audience);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLink(link);
        n.setData(data);
        repo.save(n);

        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("audience", audience);
        msg.put("userId", userId);
        msg.put("event", "notification");
        msg.put("payload", toDto(n));
        String json;
        try {
            json = objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        // Chỉ phát SSE sau khi lưu DB thành công
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redis.convertAndSend(AppConfig.REDIS_CHANNEL, json);
                }
            });
        } else {
            redis.convertAndSend(AppConfig.REDIS_CHANNEL, json);
        }
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getAudience(), n.getUserId(), n.getType(), n.getTitle(), n.getBody(),
                n.getLink(), n.getData(), n.isRead(), n.getCreatedAt());
    }
}
