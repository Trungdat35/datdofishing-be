package com.datdofishing.notification.web;

import com.datdofishing.common.security.CurrentUser;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.notification.service.NotificationService;
import com.datdofishing.notification.service.NotificationService.NotificationDto;
import com.datdofishing.notification.sse.SseHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationService notifications;
    private final SseHub hub;

    public NotificationController(NotificationService notifications, SseHub hub) {
        this.notifications = notifications;
        this.hub = hub;
    }

    /**
     * Kết nối SSE của khách hàng. Frontend dùng thư viện fetch-event-source để gửi kèm header Authorization
     * (EventSource gốc của trình duyệt không gửi được header).
     */
    @GetMapping(value = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return hub.subscribeUser(CurrentUser.id());
    }

    @GetMapping("/api/notifications")
    public PageResponse<NotificationDto> mine(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return notifications.mine(CurrentUser.id(), page, size);
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unread() {
        return Map.of("unread", notifications.unread(CurrentUser.id()));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public Map<String, Boolean> markRead(@PathVariable Long id) {
        notifications.markRead(CurrentUser.id(), id);
        return Map.of("ok", true);
    }

    @PostMapping("/api/notifications/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notifications.markAllRead(CurrentUser.id()));
    }

    // ----- Admin -----
    /** Kết nối SSE của site admin: nhận đơn mới, đơn hủy, khách mới đăng ký. */
    @GetMapping(value = "/api/admin/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter adminStream() {
        return hub.subscribeAdmin();
    }

    @GetMapping("/api/admin/notifications")
    public PageResponse<NotificationDto> admin(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return notifications.admin(page, size);
    }

    @GetMapping("/api/admin/notifications/connections")
    public Map<String, Integer> connections() {
        return Map.of("connectionsOnThisInstance", hub.connectionCount());
    }
}
