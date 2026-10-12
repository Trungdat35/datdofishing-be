package com.datdofishing.inventory.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.event.Payloads;
import com.datdofishing.common.event.Topics;
import com.datdofishing.common.outbox.OutboxWriter;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.inventory.domain.Reservation;
import com.datdofishing.inventory.domain.Stock;
import com.datdofishing.inventory.repo.ReservationRepository;
import com.datdofishing.inventory.repo.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final OutboxWriter outbox;

    public InventoryService(StockRepository stocks, ReservationRepository reservations, OutboxWriter outbox) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.outbox = outbox;
    }

    public record StockResponse(Long variantId, Long productId, int quantity, int reserved, int available,
                                int lowStockThreshold, String status) {
        static StockResponse of(Stock s) {
            return new StockResponse(s.getVariantId(), s.getProductId(), s.getQuantity(), s.getReserved(),
                    s.available(), s.getLowStockThreshold(), s.status().name());
        }
    }

    /** Bản rút gọn cho khách: chỉ số lượng còn bán và trạng thái. */
    public record PublicStock(Long variantId, int available, String status) {
    }

    @Transactional(readOnly = true)
    public List<PublicStock> publicStock(Collection<Long> variantIds) {
        return stocks.findAllById(variantIds).stream()
                .map(s -> new PublicStock(s.getVariantId(), s.available(), s.status().name())).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<StockResponse> list(int page, int size) {
        return PageResponse.of(stocks.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("variantId"))),
                StockResponse::of);
    }

    @Transactional
    public StockResponse update(Long variantId, int quantity, Integer lowStockThreshold) {
        Stock s = stocks.findById(variantId).orElseThrow(() -> BusinessException.notFound("Tồn kho của biến thể"));
        if (quantity < s.getReserved()) {
            throw BusinessException.badRequest("QUANTITY_TOO_LOW",
                    "Số lượng không được nhỏ hơn số đang giữ cho đơn (" + s.getReserved() + ")");
        }
        s.setQuantity(quantity);
        if (lowStockThreshold != null) {
            s.setLowStockThreshold(lowStockThreshold);
        }
        return StockResponse.of(s);
    }

    @Transactional
    public void createStockIfMissing(Payloads.VariantCreated e) {
        if (!stocks.existsById(e.variantId())) {
            Stock s = new Stock();
            s.setVariantId(e.variantId());
            s.setProductId(e.productId());
            s.setQuantity(0);
            stocks.save(s);
            log.info("Tạo dòng tồn kho cho biến thể {}", e.variantId());
        }
    }

    /** Bước saga: giữ hàng cho đơn mới. Đủ hàng thì phát inventory.reserved, thiếu thì inventory.rejected. */
    @Transactional
    public void reserve(Payloads.OrderCreated order) {
        if (reservations.existsByOrderId(order.orderId())) {
            return;
        }
        Map<Long, Integer> wanted = new LinkedHashMap<>();
        for (Payloads.OrderItem item : order.items()) {
            wanted.merge(item.variantId(), item.quantity(), Integer::sum);
        }
        Map<Long, Stock> locked = stocks.lockByVariantIds(wanted.keySet()).stream()
                .collect(Collectors.toMap(Stock::getVariantId, Function.identity()));

        List<String> problems = new ArrayList<>();
        wanted.forEach((variantId, qty) -> {
            Stock s = locked.get(variantId);
            if (s == null || s.available() < qty) {
                problems.add("Biến thể " + variantId + " chỉ còn " + (s == null ? 0 : s.available()));
            }
        });

        if (!problems.isEmpty()) {
            outbox.write(Topics.INVENTORY_REJECTED, String.valueOf(order.orderId()), "InventoryRejected",
                    new Payloads.InventoryResult(order.orderId(), order.code(), String.join("; ", problems)));
            log.info("Đơn {} bị từ chối: {}", order.code(), problems);
            return;
        }
        wanted.forEach((variantId, qty) -> {
            Stock s = locked.get(variantId);
            s.setReserved(s.getReserved() + qty);
            Reservation r = new Reservation();
            r.setOrderId(order.orderId());
            r.setOrderCode(order.code());
            r.setVariantId(variantId);
            r.setQuantity(qty);
            r.setStatus(Reservation.Status.RESERVED);
            reservations.save(r);
        });
        outbox.write(Topics.INVENTORY_RESERVED, String.valueOf(order.orderId()), "InventoryReserved",
                new Payloads.InventoryResult(order.orderId(), order.code(), null));
        log.info("Đã giữ hàng cho đơn {}", order.code());
    }

    /** Đơn bị hủy: trả lại hàng đã giữ. */
    @Transactional
    public void release(Long orderId) {
        settle(orderId, false);
    }

    /** Đơn đã giao: trừ hẳn khỏi kho. */
    @Transactional
    public void commit(Long orderId) {
        settle(orderId, true);
    }

    private void settle(Long orderId, boolean commit) {
        List<Reservation> rs = reservations.findByOrderIdAndStatus(orderId, Reservation.Status.RESERVED);
        if (rs.isEmpty()) {
            return;
        }
        Map<Long, Stock> locked = stocks.lockByVariantIds(rs.stream().map(Reservation::getVariantId).toList())
                .stream().collect(Collectors.toMap(Stock::getVariantId, Function.identity()));
        for (Reservation r : rs) {
            Stock s = locked.get(r.getVariantId());
            if (s != null) {
                s.setReserved(Math.max(s.getReserved() - r.getQuantity(), 0));
                if (commit) {
                    s.setQuantity(Math.max(s.getQuantity() - r.getQuantity(), 0));
                }
            }
            r.setStatus(commit ? Reservation.Status.COMMITTED : Reservation.Status.RELEASED);
        }
    }
}
