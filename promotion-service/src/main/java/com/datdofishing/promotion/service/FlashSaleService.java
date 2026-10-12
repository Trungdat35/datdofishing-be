package com.datdofishing.promotion.service;

import com.datdofishing.common.error.BusinessException;
import com.datdofishing.promotion.domain.FlashSale;
import com.datdofishing.promotion.domain.FlashSaleClaim;
import com.datdofishing.promotion.domain.FlashSaleItem;
import com.datdofishing.promotion.repo.FlashSaleClaimRepository;
import com.datdofishing.promotion.repo.FlashSaleItemRepository;
import com.datdofishing.promotion.repo.FlashSaleRepository;
import com.datdofishing.promotion.service.PromotionDtos.ClaimItem;
import com.datdofishing.promotion.service.PromotionDtos.ClaimRequest;
import com.datdofishing.promotion.service.PromotionDtos.ClaimedItem;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleItemRequest;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleItemResponse;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleRequest;
import com.datdofishing.promotion.service.PromotionDtos.FlashSaleResponse;
import com.datdofishing.promotion.service.PromotionDtos.VariantInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FlashSaleService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleService.class);

    private final FlashSaleRepository sales;
    private final FlashSaleItemRepository items;
    private final FlashSaleClaimRepository claims;
    private final FlashSaleCounter counter;
    private final RestClient catalog;

    public FlashSaleService(FlashSaleRepository sales, FlashSaleItemRepository items, FlashSaleClaimRepository claims,
                            FlashSaleCounter counter, RestClient catalogClient) {
        this.sales = sales;
        this.items = items;
        this.claims = claims;
        this.counter = counter;
        this.catalog = catalogClient;
    }

    /** Khi khởi động, nạp số suất còn lại vào Redis cho các flash sale chưa kết thúc. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void warmUp() {
        Instant now = Instant.now();
        for (FlashSale f : sales.findCurrentAndUpcoming(now)) {
            Duration ttl = Duration.between(now, f.getEndAt()).plusDays(1);
            f.getItems().forEach(i -> counter.initIfAbsent(f.getId(), i.getVariantId(), i.getQuota() - i.getSold(), ttl));
        }
        log.info("Đã nạp bộ đếm flash sale vào Redis");
    }

    /** Flash sale đang chạy (hoặc sắp chạy) cho trang chủ, kèm số suất còn lại. */
    @Transactional(readOnly = true)
    public FlashSaleResponse current() {
        Instant now = Instant.now();
        return sales.findCurrentAndUpcoming(now).stream().findFirst().map(f -> toResponse(f, now)).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<FlashSaleResponse> all() {
        Instant now = Instant.now();
        return sales.findAllByOrderByStartAtDesc().stream().map(f -> toResponse(f, now)).toList();
    }

    @Transactional(readOnly = true)
    public FlashSaleResponse get(Long id) {
        return toResponse(sales.findById(id).orElseThrow(() -> BusinessException.notFound("Flash sale")), Instant.now());
    }

    @Transactional
    public FlashSaleResponse save(Long id, FlashSaleRequest req) {
        if (!req.endAt().isAfter(req.startAt())) {
            throw BusinessException.badRequest("INVALID_TIME", "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        FlashSale f = id == null ? new FlashSale()
                : sales.findById(id).orElseThrow(() -> BusinessException.notFound("Flash sale"));
        f.setName(req.name().trim());
        f.setStartAt(req.startAt());
        f.setEndAt(req.endAt());
        f.setActive(req.active() == null || req.active());

        Map<Long, VariantInfo> infos = variantInfos(req.items().stream().map(FlashSaleItemRequest::variantId).toList());
        Map<Long, FlashSaleItem> existing = f.getItems().stream()
                .collect(Collectors.toMap(FlashSaleItem::getVariantId, Function.identity()));
        List<FlashSaleItem> next = new ArrayList<>();
        for (FlashSaleItemRequest r : req.items()) {
            VariantInfo v = infos.get(r.variantId());
            if (v == null) {
                throw BusinessException.notFound("Biến thể " + r.variantId());
            }
            if (r.salePrice() >= v.price()) {
                throw BusinessException.badRequest("INVALID_PRICE",
                        "Giá flash sale của " + v.productName() + " phải thấp hơn giá bán " + v.price());
            }
            FlashSaleItem item = existing.getOrDefault(r.variantId(), new FlashSaleItem());
            if (r.quota() < item.getSold()) {
                throw BusinessException.badRequest("INVALID_QUOTA", "Số suất không được nhỏ hơn số đã bán");
            }
            item.setFlashSale(f);
            item.setProductId(v.productId());
            item.setVariantId(v.variantId());
            item.setProductName(v.productName());
            item.setProductSlug(v.productSlug());
            item.setVariantName(v.variantName());
            item.setImageUrl(v.imageUrl());
            item.setOriginalPrice(v.price());
            item.setSalePrice(r.salePrice());
            item.setQuota(r.quota());
            item.setSortOrder(r.sortOrder());
            next.add(item);
        }
        f.getItems().clear();
        f.getItems().addAll(next);
        if (f.getId() == null) {
            sales.save(f);
        }
        sales.flush();

        Duration ttl = Duration.between(Instant.now(), f.getEndAt()).plusDays(1);
        if (!ttl.isNegative()) {
            f.getItems().forEach(i -> counter.init(f.getId(), i.getVariantId(), i.getQuota() - i.getSold(), ttl));
        }
        return toResponse(f, Instant.now());
    }

    @Transactional
    public void deactivate(Long id) {
        FlashSale f = sales.findById(id).orElseThrow(() -> BusinessException.notFound("Flash sale"));
        f.setActive(false);
    }

    /**
     * Giữ suất flash sale cho một đơn. Mỗi biến thể: lấy đủ số lượng thì hưởng giá sale, không đủ thì giá thường.
     */
    @Transactional
    public List<ClaimedItem> claim(ClaimRequest req) {
        if (req.items() == null || req.items().isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        Map<Long, FlashSaleItem> running = items.findRunningByVariantIds(now,
                        req.items().stream().map(ClaimItem::variantId).toList())
                .stream().collect(Collectors.toMap(FlashSaleItem::getVariantId, Function.identity(), (a, b) -> a));
        List<ClaimedItem> result = new ArrayList<>();
        for (ClaimItem ci : req.items()) {
            FlashSaleItem item = running.get(ci.variantId());
            if (item == null || ci.quantity() <= 0) {
                continue;
            }
            Long saleId = item.getFlashSale().getId();
            long r = counter.take(saleId, item.getVariantId(), ci.quantity());
            if (r == -1) {
                Duration ttl = Duration.between(now, item.getFlashSale().getEndAt()).plusDays(1);
                counter.initIfAbsent(saleId, item.getVariantId(), item.getQuota() - item.getSold(), ttl);
                r = counter.take(saleId, item.getVariantId(), ci.quantity());
            }
            if (r == 1) {
                item.setSold(item.getSold() + ci.quantity());
                FlashSaleClaim c = new FlashSaleClaim();
                c.setOrderCode(req.orderCode());
                c.setFlashSaleId(saleId);
                c.setVariantId(item.getVariantId());
                c.setQuantity(ci.quantity());
                c.setStatus(FlashSaleClaim.CLAIMED);
                claims.save(c);
                result.add(new ClaimedItem(item.getVariantId(), ci.quantity(), item.getSalePrice(), saleId));
            }
        }
        return result;
    }

    /** Trả lại suất khi đơn bị hủy. Gọi nhiều lần vẫn an toàn. */
    @Transactional
    public void release(String orderCode) {
        for (FlashSaleClaim c : claims.findByOrderCodeAndStatus(orderCode, FlashSaleClaim.CLAIMED)) {
            items.findByFlashSaleIdAndVariantId(c.getFlashSaleId(), c.getVariantId()).ifPresent(i ->
                    i.setSold(Math.max(i.getSold() - c.getQuantity(), 0)));
            counter.giveBack(c.getFlashSaleId(), c.getVariantId(), c.getQuantity());
            c.setStatus(FlashSaleClaim.RELEASED);
            log.info("Trả {} suất flash sale biến thể {} của đơn {}", c.getQuantity(), c.getVariantId(), orderCode);
        }
    }

    private Map<Long, VariantInfo> variantInfos(List<Long> ids) {
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<VariantInfo> list = catalog.get()
                .uri(u -> u.path("/internal/variants").queryParam("ids", joined).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<VariantInfo>>() {
                });
        return list == null ? Map.of()
                : list.stream().collect(Collectors.toMap(VariantInfo::variantId, Function.identity(), (a, b) -> a));
    }

    private FlashSaleResponse toResponse(FlashSale f, Instant now) {
        List<FlashSaleItemResponse> list = f.getItems().stream().map(i -> {
            int remaining = counter.remaining(f.getId(), i.getVariantId(), i.getQuota() - i.getSold());
            int discount = i.getOriginalPrice() <= 0 ? 0
                    : (int) Math.round((i.getOriginalPrice() - i.getSalePrice()) * 100.0 / i.getOriginalPrice());
            return new FlashSaleItemResponse(i.getId(), i.getProductId(), i.getVariantId(), i.getProductName(),
                    i.getProductSlug(), i.getVariantName(), i.getImageUrl(), i.getOriginalPrice(), i.getSalePrice(),
                    discount, i.getQuota(), i.getSold(), remaining, remaining <= 0);
        }).toList();
        return new FlashSaleResponse(f.getId(), f.getName(), f.getStartAt(), f.getEndAt(), f.isActive(),
                f.runningAt(now), now, list);
    }
}
