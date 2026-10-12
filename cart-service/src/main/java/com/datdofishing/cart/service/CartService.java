package com.datdofishing.cart.service;

import com.datdofishing.common.error.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Giỏ hàng lưu Redis dạng hash: cart:{userId} -> { variantId: quantity }.
 * Giá và tên luôn lấy mới từ catalog-service khi xem giỏ.
 */
@Service
public class CartService {

    private final StringRedisTemplate redis;
    private final RestClient catalog;
    private final int maxQuantity;
    private final Duration ttl;

    public CartService(StringRedisTemplate redis, RestClient catalogClient,
                       @Value("${datdofishing.cart.max-quantity}") int maxQuantity,
                       @Value("${datdofishing.cart.ttl-days}") int ttlDays) {
        this.redis = redis;
        this.catalog = catalogClient;
        this.maxQuantity = maxQuantity;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public record CartItem(Long variantId, int quantity, Long productId, String productName, String productSlug,
                           String variantName, Long price, Long compareAtPrice, String imageUrl, boolean available,
                           long lineTotal) {
    }

    public record Cart(List<CartItem> items, int totalQuantity, long subtotal) {
    }

    public record ItemInput(Long variantId, int quantity) {
    }

    public Cart get(Long userId) {
        Map<String, String> raw = hash().entries(key(userId));
        if (raw.isEmpty()) {
            return new Cart(List.of(), 0, 0);
        }
        Map<Long, VariantInfo> infos = fetch(raw.keySet().stream().map(Long::valueOf).toList());
        List<CartItem> items = new ArrayList<>();
        int totalQty = 0;
        long subtotal = 0;
        for (Map.Entry<String, String> e : raw.entrySet()) {
            Long variantId = Long.valueOf(e.getKey());
            int qty = Integer.parseInt(e.getValue());
            VariantInfo v = infos.get(variantId);
            if (v == null) {
                // Biến thể đã bị xóa khỏi catalog -> bỏ khỏi giỏ
                hash().delete(key(userId), e.getKey());
                continue;
            }
            long line = v.available() ? v.price() * qty : 0;
            items.add(new CartItem(variantId, qty, v.productId(), v.productName(), v.productSlug(), v.variantName(),
                    v.price(), v.compareAtPrice(), v.imageUrl(), v.available(), line));
            if (v.available()) {
                totalQty += qty;
                subtotal += line;
            }
        }
        return new Cart(items, totalQty, subtotal);
    }

    public Cart add(Long userId, ItemInput in) {
        ensureVariant(in.variantId());
        String current = hash().get(key(userId), String.valueOf(in.variantId()));
        int qty = (current == null ? 0 : Integer.parseInt(current)) + Math.max(in.quantity(), 1);
        put(userId, in.variantId(), qty);
        return get(userId);
    }

    public Cart setQuantity(Long userId, Long variantId, int quantity) {
        if (quantity <= 0) {
            hash().delete(key(userId), String.valueOf(variantId));
        } else {
            ensureVariant(variantId);
            put(userId, variantId, quantity);
        }
        return get(userId);
    }

    public Cart remove(Long userId, Long variantId) {
        hash().delete(key(userId), String.valueOf(variantId));
        return get(userId);
    }

    public void clear(Long userId) {
        redis.delete(key(userId));
    }

    /** Gộp giỏ hàng lưu ở trình duyệt (khi chưa đăng nhập) vào giỏ trên server. */
    public Cart merge(Long userId, List<ItemInput> items) {
        if (items != null) {
            Map<Long, VariantInfo> infos = fetch(items.stream().map(ItemInput::variantId).toList());
            for (ItemInput in : items) {
                if (infos.containsKey(in.variantId()) && in.quantity() > 0) {
                    String current = hash().get(key(userId), String.valueOf(in.variantId()));
                    int qty = Math.max(current == null ? 0 : Integer.parseInt(current), in.quantity());
                    put(userId, in.variantId(), qty);
                }
            }
        }
        return get(userId);
    }

    /** Sau khi đặt hàng, bỏ các sản phẩm đã đặt khỏi giỏ. */
    public void removeVariants(Long userId, Collection<Long> variantIds) {
        Object[] fields = variantIds.stream().map(String::valueOf).toArray();
        if (fields.length > 0) {
            hash().delete(key(userId), fields);
        }
    }

    private void put(Long userId, Long variantId, int qty) {
        if (qty > maxQuantity) {
            throw BusinessException.badRequest("QUANTITY_TOO_LARGE", "Mỗi sản phẩm tối đa " + maxQuantity);
        }
        hash().put(key(userId), String.valueOf(variantId), String.valueOf(qty));
        redis.expire(key(userId), ttl);
    }

    private void ensureVariant(Long variantId) {
        VariantInfo v = fetch(List.of(variantId)).get(variantId);
        if (v == null) {
            throw BusinessException.notFound("Sản phẩm");
        }
        if (!v.available()) {
            throw BusinessException.badRequest("PRODUCT_UNAVAILABLE", "Sản phẩm đã ngừng bán");
        }
    }

    private Map<Long, VariantInfo> fetch(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<VariantInfo> list = catalog.get()
                .uri(uri -> uri.path("/internal/variants").queryParam("ids", joined).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<VariantInfo>>() {
                });
        return list == null ? Map.of()
                : list.stream().collect(Collectors.toMap(VariantInfo::variantId, Function.identity(), (a, b) -> a));
    }

    private HashOperations<String, String, String> hash() {
        return redis.opsForHash();
    }

    private static String key(Long userId) {
        return "cart:" + userId;
    }
}
