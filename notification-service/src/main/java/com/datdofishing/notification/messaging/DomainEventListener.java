package com.datdofishing.notification.messaging;

import com.datdofishing.common.event.EventEnvelope;
import com.datdofishing.common.event.EventParser;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.ProcessedEventStore;
import com.datdofishing.common.event.Topics;
import com.datdofishing.notification.email.EmailJob;
import com.datdofishing.notification.email.EmailQueue;
import com.datdofishing.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nghe sự kiện nghiệp vụ trên Kafka rồi:
 * - đẩy việc gửi email vào RabbitMQ
 * - lưu thông báo + phát SSE cho khách hàng / admin
 */
@Component
public class DomainEventListener {

    private static final Map<String, String> STATUS_TEXT = Map.of(
            "PENDING", "Đang chờ xác nhận",
            "CONFIRMED", "Đã xác nhận",
            "SHIPPING", "Đang giao hàng",
            "DELIVERED", "Đã giao thành công",
            "CANCELLED", "Đã hủy");

    private final EventParser parser;
    private final ProcessedEventStore processed;
    private final EmailQueue emails;
    private final NotificationService notifications;
    private final String shopUrl;
    private final String adminUrl;
    private final String publicApiUrl;

    public DomainEventListener(EventParser parser, ProcessedEventStore processed, EmailQueue emails,
                               NotificationService notifications,
                               @Value("${datdofishing.shop-url}") String shopUrl,
                               @Value("${datdofishing.admin-url}") String adminUrl,
                               @Value("${datdofishing.public-api-url}") String publicApiUrl) {
        this.parser = parser;
        this.processed = processed;
        this.emails = emails;
        this.notifications = notifications;
        this.shopUrl = shopUrl;
        this.adminUrl = adminUrl;
        this.publicApiUrl = publicApiUrl;
    }

    /** Đăng ký thành công -> gửi email thông tin tài khoản (không bao giờ gửi mật khẩu). */
    @KafkaListener(topics = Topics.USER_REGISTERED)
    @Transactional
    public void onUserRegistered(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.UserRegistered u = parser.payload(e, Payloads.UserRegistered.class);
        Map<String, Object> vars = new HashMap<>();
        vars.put("fullName", u.fullName());
        vars.put("email", u.email());
        vars.put("phone", u.phone());
        vars.put("provider", "GOOGLE".equals(u.provider()) ? "Google" : "Email và mật khẩu");
        vars.put("verifyUrl", u.verifyToken() == null ? null
                : publicApiUrl + "/api/auth/verify-email?token=" + u.verifyToken());
        vars.put("shopUrl", shopUrl);
        emails.enqueue(new EmailJob(u.email(), "Chào mừng bạn đến với DatDo Fishing", "welcome", vars));

        notifications.toUser(u.userId(), "WELCOME", "Chào mừng " + u.fullName(),
                "Tài khoản của bạn đã được tạo thành công.", shopUrl, null);
        notifications.toAdmins("NEW_USER", "Khách hàng mới đăng ký", u.fullName() + " (" + u.email() + ")",
                adminUrl + "/users/" + u.userId(), Map.of("userId", u.userId()));
    }

    @KafkaListener(topics = Topics.USER_PASSWORD_RESET)
    @Transactional
    public void onPasswordReset(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.PasswordResetRequested p = parser.payload(e, Payloads.PasswordResetRequested.class);
        Map<String, Object> vars = new HashMap<>();
        vars.put("fullName", p.fullName());
        vars.put("resetUrl", shopUrl + "/reset-password?token=" + p.resetToken());
        emails.enqueue(new EmailJob(p.email(), "Đặt lại mật khẩu DatDo Fishing", "reset-password", vars));
    }

    /** Đơn mới -> báo realtime cho admin, báo khách đã nhận đơn. */
    @KafkaListener(topics = Topics.ORDER_CREATED)
    @Transactional
    public void onOrderCreated(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.OrderCreated o = parser.payload(e, Payloads.OrderCreated.class);
        Map<String, Object> data = Map.of("orderId", o.orderId(), "code", o.code(), "status", "PENDING",
                "total", o.total());
        notifications.toAdmins("ORDER_CREATED", "Đơn hàng mới " + o.code(),
                (o.userName() == null ? "Khách hàng" : o.userName()) + " vừa đặt đơn " + money(o.total()),
                adminUrl + "/orders/" + o.code(), data);
        notifications.toUser(o.userId(), "ORDER_CREATED", "Đã nhận đơn " + o.code(),
                "Chúng tôi đang kiểm tra kho cho đơn hàng của bạn.", shopUrl + "/orders/" + o.code(), data);
    }

    /** Đơn đổi trạng thái -> báo realtime cho khách, gửi email. Đơn bị hủy thì báo cả admin. */
    @KafkaListener(topics = Topics.ORDER_STATUS_CHANGED)
    @Transactional
    public void onOrderStatusChanged(String message) {
        EventEnvelope e = parser.parse(message);
        if (!processed.markProcessed(e.eventId())) {
            return;
        }
        Payloads.OrderStatusChanged o = parser.payload(e, Payloads.OrderStatusChanged.class);
        if (o.status().equals(o.previousStatus())) {
            return;
        }
        String statusText = STATUS_TEXT.getOrDefault(o.status(), o.status());
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", o.orderId());
        data.put("code", o.code());
        data.put("status", o.status());
        data.put("previousStatus", o.previousStatus());
        String body = "CANCELLED".equals(o.status()) && o.reason() != null
                ? "Đơn " + o.code() + " đã hủy: " + o.reason()
                : "Đơn " + o.code() + ": " + statusText;
        notifications.toUser(o.userId(), "ORDER_STATUS", "Cập nhật đơn " + o.code(), body,
                shopUrl + "/orders/" + o.code(), data);
        if ("CANCELLED".equals(o.status())) {
            notifications.toAdmins("ORDER_CANCELLED", "Đơn " + o.code() + " đã hủy", o.reason(),
                    adminUrl + "/orders/" + o.code(), data);
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("fullName", o.userName());
        vars.put("code", o.code());
        vars.put("statusText", statusText);
        vars.put("reason", o.reason());
        vars.put("total", money(o.total()));
        List<Map<String, Object>> items = o.items() == null ? List.of() : o.items().stream().map(i -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", i.productName() + (i.variantName() == null ? "" : " - " + i.variantName()));
            m.put("quantity", i.quantity());
            m.put("price", money(i.unitPrice()));
            return m;
        }).toList();
        vars.put("items", items);
        vars.put("orderUrl", shopUrl + "/orders/" + o.code());
        emails.enqueue(new EmailJob(o.userEmail(), "Đơn hàng " + o.code() + ": " + statusText, "order-status", vars));
    }

    private static String money(long vnd) {
        return NumberFormat.getInstance(Locale.of("vi", "VN")).format(vnd) + "₫";
    }
}
