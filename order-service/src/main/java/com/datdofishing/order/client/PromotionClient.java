package com.datdofishing.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Gọi promotion-service để lấy suất flash sale. Nếu promotion lỗi thì vẫn cho đặt hàng với giá thường. */
@Component
public class PromotionClient {

    private static final Logger log = LoggerFactory.getLogger(PromotionClient.class);

    public record ClaimItem(Long variantId, int quantity) {
    }

    public record ClaimRequest(String orderCode, Long userId, List<ClaimItem> items) {
    }

    public record ClaimedItem(Long variantId, int quantity, long salePrice, Long flashSaleId) {
    }

    private final RestClient client;

    public PromotionClient(@Qualifier("promotionRestClient") RestClient client) {
        this.client = client;
    }

    public Map<Long, ClaimedItem> claim(String orderCode, Long userId, List<ClaimItem> items) {
        try {
            List<ClaimedItem> claimed = client.post()
                    .uri("/internal/flash-sales/claim")
                    .body(new ClaimRequest(orderCode, userId, items))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ClaimedItem>>() {
                    });
            return claimed == null ? Map.of()
                    : claimed.stream().collect(Collectors.toMap(ClaimedItem::variantId, Function.identity()));
        } catch (RestClientException e) {
            log.warn("Không gọi được promotion-service, đặt hàng với giá thường: {}", e.getMessage());
            return Map.of();
        }
    }

    public void release(String orderCode) {
        try {
            client.post().uri("/internal/flash-sales/release/{code}", orderCode).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Không trả lại được suất flash sale của đơn {}: {}", orderCode, e.getMessage());
        }
    }
}
