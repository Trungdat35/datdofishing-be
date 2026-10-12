package com.datdofishing.order.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.Topics;
import com.datdofishing.common.outbox.OutboxWriter;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.order.client.CatalogClient;
import com.datdofishing.order.client.CatalogClient.VariantInfo;
import com.datdofishing.order.client.PromotionClient;
import com.datdofishing.order.client.PromotionClient.ClaimItem;
import com.datdofishing.order.client.PromotionClient.ClaimedItem;
import com.datdofishing.order.domain.Order;
import com.datdofishing.order.domain.OrderItem;
import com.datdofishing.order.domain.OrderStatus;
import com.datdofishing.order.repo.OrderRepository;
import com.datdofishing.order.service.OrderDtos.CreateOrderRequest;
import com.datdofishing.order.service.OrderDtos.HistoryResponse;
import com.datdofishing.order.service.OrderDtos.ItemRequest;
import com.datdofishing.order.service.OrderDtos.ItemResponse;
import com.datdofishing.order.service.OrderDtos.OrderResponse;
import com.datdofishing.order.service.OrderDtos.OrderSummary;
import com.datdofishing.order.service.OrderDtos.StatusStat;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orders;
    private final CatalogClient catalog;
    private final PromotionClient promotion;
    private final OutboxWriter outbox;
    private final String instanceId;
    private final long freeShippingFrom;
    private final long shippingFee;

    public OrderService(OrderRepository orders, CatalogClient catalog, PromotionClient promotion, OutboxWriter outbox,
                        @Value("${datdofishing.instance-id}") String instanceId,
                        @Value("${datdofishing.order.free-shipping-from}") long freeShippingFrom,
                        @Value("${datdofishing.order.shipping-fee}") long shippingFee) {
        this.orders = orders;
        this.catalog = catalog;
        this.promotion = promotion;
        this.outbox = outbox;
        this.instanceId = instanceId;
        this.freeShippingFrom = freeShippingFrom;
        this.shippingFee = shippingFee;
    }

    /** Thông tin người đặt lấy từ JWT. */
    public record Buyer(Long id, String email, String name) {
    }

    public String newCode() {
        StringBuilder sb = new StringBuilder("DD").append(LocalDate.now(VN).format(DateTimeFormatter.ofPattern("yyMMdd")));
        sb.append('-');
        for (int i = 0; i < 6; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Tạo đơn ở trạng thái PENDING và ghi sự kiện order.created vào outbox.
     * Kho (inventory-service) sẽ giữ hàng rồi báo lại qua Kafka.
     */
    @Transactional
    public OrderResponse create(Buyer buyer, CreateOrderRequest req, String code,
                                Map<Long, ClaimedItem> flashClaims) {
        Map<Long, Integer> qtyByVariant = new LinkedHashMap<>();
        for (ItemRequest i : req.items()) {
            qtyByVariant.merge(i.variantId(), i.quantity(), Integer::sum);
        }
        Map<Long, VariantInfo> infos = catalog.variants(qtyByVariant.keySet());

        Order o = new Order();
        o.setCode(code);
        o.setUserId(buyer.id());
        o.setUserEmail(buyer.email());
        o.setUserName(buyer.name());
        o.setStatus(OrderStatus.PENDING);
        o.setPaymentMethod(StringUtils.hasText(req.paymentMethod()) ? req.paymentMethod().toUpperCase(Locale.ROOT) : "COD");
        o.setRecipientName(req.shipping().recipientName().trim());
        o.setPhone(req.shipping().phone());
        o.setShippingAddress(req.shipping().address().trim());
        o.setNote(req.note());
        o.setCreatedByInstance(instanceId);

        long subtotal = 0;
        long discount = 0;
        for (Map.Entry<Long, Integer> e : qtyByVariant.entrySet()) {
            VariantInfo v = infos.get(e.getKey());
            if (v == null || !v.available()) {
                throw BusinessException.badRequest("PRODUCT_UNAVAILABLE",
                        "Sản phẩm " + (v == null ? e.getKey() : v.productName() + " - " + v.variantName())
                                + " hiện không bán");
            }
            int qty = e.getValue();
            ClaimedItem flash = flashClaims.get(v.variantId());
            boolean isFlash = flash != null && flash.quantity() == qty;
            long unit = isFlash ? flash.salePrice() : v.price();
            OrderItem item = new OrderItem();
            item.setProductId(v.productId());
            item.setVariantId(v.variantId());
            item.setProductName(v.productName());
            item.setVariantName(v.variantName());
            item.setSku(v.sku());
            item.setImageUrl(v.imageUrl());
            item.setOriginalPrice(v.price());
            item.setUnitPrice(unit);
            item.setFlashSale(isFlash);
            item.setQuantity(qty);
            item.setLineTotal(unit * qty);
            o.addItem(item);
            subtotal += v.price() * qty;
            discount += (v.price() - unit) * qty;
        }
        long afterDiscount = subtotal - discount;
        o.setSubtotal(subtotal);
        o.setDiscount(discount);
        o.setShippingFee(afterDiscount >= freeShippingFrom ? 0 : shippingFee);
        o.setTotal(afterDiscount + o.getShippingFee());
        o.addHistory(null, OrderStatus.PENDING, "Khách đặt hàng", "customer:" + buyer.id());
        orders.save(o);

        outbox.write(Topics.ORDER_CREATED, String.valueOf(o.getId()), "OrderCreated",
                new Payloads.OrderCreated(o.getId(), o.getCode(), o.getUserId(), o.getUserEmail(), o.getUserName(),
                        o.getTotal(), toPayloadItems(o)));
        log.info("[{}] Tạo đơn {} cho user {}", instanceId, o.getCode(), buyer.id());
        return toResponse(o);
    }

    /** Đặt suất flash sale trước khi tạo đơn (gọi ngoài transaction DB). */
    public Map<Long, ClaimedItem> claimFlashSale(String code, Long userId, List<ItemRequest> items) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        items.forEach(i -> qty.merge(i.variantId(), i.quantity(), Integer::sum));
        List<ClaimItem> claimItems = qty.entrySet().stream().map(e -> new ClaimItem(e.getKey(), e.getValue())).toList();
        return promotion.claim(code, userId, claimItems);
    }

    public void releaseFlashSale(String code) {
        promotion.release(code);
    }

    @Transactional
    public void changeStatus(Long orderId, OrderStatus target, String note, String by) {
        Order o = orders.findById(orderId).orElseThrow(() -> BusinessException.notFound("Đơn hàng"));
        move(o, target, note, by);
    }

    @Transactional
    public OrderResponse changeStatusByCode(String code, OrderStatus target, String note, String by) {
        Order o = getByCode(code);
        move(o, target, note, by);
        return toResponse(o);
    }

    @Transactional
    public OrderResponse cancelByCustomer(Long userId, String code, String reason) {
        Order o = getByCode(code);
        if (!o.getUserId().equals(userId)) {
            throw BusinessException.notFound("Đơn hàng");
        }
        if (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.CONFIRMED) {
            throw BusinessException.badRequest("CANNOT_CANCEL", "Đơn đang giao hoặc đã hoàn tất, không thể hủy");
        }
        move(o, OrderStatus.CANCELLED, StringUtils.hasText(reason) ? reason : "Khách hủy đơn", "customer:" + userId);
        return toResponse(o);
    }

    /** Kho đã giữ hàng: PENDING -> CONFIRMED. Nếu đơn đã bị hủy trước đó thì phát lại sự kiện hủy để kho trả hàng. */
    @Transactional
    public void onInventoryReserved(Long orderId) {
        Order o = orders.findById(orderId).orElse(null);
        if (o == null) {
            return;
        }
        if (o.getStatus() == OrderStatus.PENDING) {
            move(o, OrderStatus.CONFIRMED, "Kho đã giữ hàng", "inventory");
        } else if (o.getStatus() == OrderStatus.CANCELLED) {
            outbox.write(Topics.ORDER_STATUS_CHANGED, String.valueOf(o.getId()), "OrderStatusChanged",
                    new Payloads.OrderStatusChanged(o.getId(), o.getCode(), o.getUserId(), o.getUserEmail(),
                            o.getUserName(), OrderStatus.CANCELLED.name(), OrderStatus.CANCELLED.name(), o.getTotal(),
                            "Đơn đã hủy trước khi kho giữ hàng", toPayloadItems(o)));
        }
    }

    /** Kho báo thiếu hàng: hủy đơn. */
    @Transactional
    public void onInventoryRejected(Long orderId, String reason) {
        Order o = orders.findById(orderId).orElse(null);
        if (o != null && o.getStatus() == OrderStatus.PENDING) {
            move(o, OrderStatus.CANCELLED, "Hết hàng: " + reason, "inventory");
        }
    }

    /** Hủy các đơn PENDING quá lâu (ví dụ inventory-service không phản hồi). */
    @Transactional
    public int cancelStalePending(java.time.Instant before) {
        List<Order> stale = orders.findTop100ByStatusAndCreatedAtBefore(OrderStatus.PENDING, before);
        stale.forEach(o -> move(o, OrderStatus.CANCELLED, "Quá thời gian chờ giữ hàng", "system"));
        return stale.size();
    }

    private void move(Order o, OrderStatus target, String note, String by) {
        OrderStatus from = o.getStatus();
        if (from == target) {
            return;
        }
        if (!from.canMoveTo(target)) {
            throw BusinessException.badRequest("INVALID_STATUS",
                    "Không thể chuyển đơn từ " + from + " sang " + target);
        }
        o.setStatus(target);
        if (target == OrderStatus.CANCELLED) {
            o.setCancelReason(note);
        }
        o.addHistory(from, target, note, by);
        outbox.write(Topics.ORDER_STATUS_CHANGED, String.valueOf(o.getId()), "OrderStatusChanged",
                new Payloads.OrderStatusChanged(o.getId(), o.getCode(), o.getUserId(), o.getUserEmail(),
                        o.getUserName(), target.name(), from.name(), o.getTotal(), note, toPayloadItems(o)));
        log.info("[{}] Đơn {}: {} -> {}", instanceId, o.getCode(), from, target);
    }

    @Transactional(readOnly = true)
    public OrderResponse getForUser(Long userId, String code, boolean admin) {
        Order o = getByCode(code);
        if (!admin && !o.getUserId().equals(userId)) {
            throw BusinessException.notFound("Đơn hàng");
        }
        return toResponse(o);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(Long userId, int page, int size) {
        return PageResponse.of(orders.findByUserId(userId,
                PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"))), this::toSummary);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> adminSearch(String status, String q, int page, int size) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                ps.add(cb.equal(root.get("status"), OrderStatus.valueOf(status.toUpperCase(Locale.ROOT))));
            }
            if (StringUtils.hasText(q)) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("code")), like), cb.like(cb.lower(root.get("phone")), like),
                        cb.like(cb.lower(root.get("userEmail")), like),
                        cb.like(cb.lower(root.get("recipientName")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageResponse.of(orders.findAll(spec,
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"))), this::toSummary);
    }

    @Transactional(readOnly = true)
    public List<StatusStat> stats() {
        return orders.statsByStatus().stream()
                .map(r -> new StatusStat(String.valueOf(r[0]), ((Number) r[1]).longValue(), ((Number) r[2]).longValue()))
                .toList();
    }

    private Order getByCode(String code) {
        return orders.findByCode(code).orElseThrow(() -> BusinessException.notFound("Đơn hàng"));
    }

    private static List<Payloads.OrderItem> toPayloadItems(Order o) {
        return o.getItems().stream().map(i -> new Payloads.OrderItem(i.getVariantId(), i.getProductId(),
                i.getProductName(), i.getVariantName(), i.getQuantity(), i.getUnitPrice())).toList();
    }

    private OrderSummary toSummary(Order o) {
        OrderItem first = o.getItems().isEmpty() ? null : o.getItems().get(0);
        return new OrderSummary(o.getId(), o.getCode(), o.getStatus().name(), o.getTotal(),
                o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                first == null ? null : first.getProductName(), first == null ? null : first.getImageUrl(),
                o.getRecipientName(), o.getCreatedAt());
    }

    private OrderResponse toResponse(Order o) {
        List<ItemResponse> items = o.getItems().stream().map(i -> new ItemResponse(i.getProductId(), i.getVariantId(),
                i.getProductName(), i.getVariantName(), i.getSku(), i.getImageUrl(), i.getUnitPrice(),
                i.getOriginalPrice(), i.isFlashSale(), i.getQuantity(), i.getLineTotal())).toList();
        List<HistoryResponse> history = o.getHistory().stream().map(h -> new HistoryResponse(h.getFromStatus(),
                h.getToStatus(), h.getNote(), h.getChangedBy(), h.getCreatedAt())).toList();
        return new OrderResponse(o.getId(), o.getCode(), o.getUserId(), o.getUserEmail(), o.getUserName(),
                o.getStatus().name(), o.getSubtotal(), o.getDiscount(), o.getShippingFee(), o.getTotal(),
                o.getPaymentMethod(), o.getRecipientName(), o.getPhone(), o.getShippingAddress(), o.getNote(),
                o.getCancelReason(), o.getCreatedByInstance(), o.getCreatedAt(), o.getUpdatedAt(), items, history);
    }
}
