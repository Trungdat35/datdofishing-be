package com.datdofishing.order.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.order.client.PromotionClient.ClaimedItem;
import com.datdofishing.order.service.OrderDtos.CreateOrderRequest;
import com.datdofishing.order.service.OrderDtos.OrderResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;

/**
 * Điều phối đặt hàng:
 * 1. Idempotency-Key (Redis) để bấm "Đặt hàng" 2 lần hoặc retry mạng không tạo 2 đơn, kể cả khi 2 request
 *    rơi vào 2 instance khác nhau.
 * 2. Giữ suất flash sale ở promotion-service.
 * 3. Tạo đơn + outbox trong 1 transaction. Lỗi thì trả lại suất flash sale.
 */
@Service
public class PlaceOrderService {

    private static final Duration IDEM_TTL = Duration.ofHours(24);
    private static final String PROCESSING = "PROCESSING";
    private static final String DONE_PREFIX = "ORDER:";

    private final OrderService orders;
    private final StringRedisTemplate redis;

    public PlaceOrderService(OrderService orders, StringRedisTemplate redis) {
        this.orders = orders;
        this.redis = redis;
    }

    public record Result(OrderResponse order, boolean replayed) {
    }

    public Result place(OrderService.Buyer buyer, CreateOrderRequest req, String idempotencyKey) {
        String idemKey = StringUtils.hasText(idempotencyKey) ? "idem:order:" + buyer.id() + ":" + idempotencyKey : null;
        if (idemKey != null) {
            Boolean first = redis.opsForValue().setIfAbsent(idemKey, PROCESSING, IDEM_TTL);
            if (!Boolean.TRUE.equals(first)) {
                String value = redis.opsForValue().get(idemKey);
                if (value != null && value.startsWith(DONE_PREFIX)) {
                    return new Result(orders.getForUser(buyer.id(), value.substring(DONE_PREFIX.length()), false), true);
                }
                throw BusinessException.conflict("ORDER_IN_PROGRESS", "Đơn hàng đang được xử lý, vui lòng chờ");
            }
        }
        String code = orders.newCode();
        Map<Long, ClaimedItem> claims = orders.claimFlashSale(code, buyer.id(), req.items());
        try {
            OrderResponse created = orders.create(buyer, req, code, claims);
            if (idemKey != null) {
                redis.opsForValue().set(idemKey, DONE_PREFIX + created.code(), IDEM_TTL);
            }
            return new Result(created, false);
        } catch (RuntimeException e) {
            if (!claims.isEmpty()) {
                orders.releaseFlashSale(code);
            }
            if (idemKey != null) {
                redis.delete(idemKey);
            }
            throw e;
        }
    }
}
