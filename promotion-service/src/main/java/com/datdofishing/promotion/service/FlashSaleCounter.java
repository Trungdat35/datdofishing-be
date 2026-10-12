package com.datdofishing.promotion.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Bộ đếm suất flash sale trong Redis. Dùng Lua script để "kiểm tra còn suất rồi trừ" là một thao tác nguyên tử,
 * nhiều request đồng thời không thể bán vượt số suất.
 * Key: fs:{flashSaleId}:{variantId} -> số suất còn lại.
 */
@Component
public class FlashSaleCounter {

    private static final DefaultRedisScript<Long> TAKE = new DefaultRedisScript<>("""
            local remaining = redis.call('GET', KEYS[1])
            if not remaining then return -1 end
            if tonumber(remaining) >= tonumber(ARGV[1]) then
              redis.call('DECRBY', KEYS[1], ARGV[1])
              return 1
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    public FlashSaleCounter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** @return 1 = lấy được suất, 0 = hết suất, -1 = chưa có key (cần nạp từ DB) */
    public long take(Long saleId, Long variantId, int qty) {
        Long r = redis.execute(TAKE, List.of(key(saleId, variantId)), String.valueOf(qty));
        return r == null ? -1 : r;
    }

    public void giveBack(Long saleId, Long variantId, int qty) {
        redis.opsForValue().increment(key(saleId, variantId), qty);
    }

    public void init(Long saleId, Long variantId, int remaining, Duration ttl) {
        redis.opsForValue().set(key(saleId, variantId), String.valueOf(Math.max(remaining, 0)), ttl);
    }

    /** Chỉ nạp nếu chưa có, tránh ghi đè số đang đếm. */
    public void initIfAbsent(Long saleId, Long variantId, int remaining, Duration ttl) {
        redis.opsForValue().setIfAbsent(key(saleId, variantId), String.valueOf(Math.max(remaining, 0)), ttl);
    }

    public int remaining(Long saleId, Long variantId, int fallback) {
        String v = redis.opsForValue().get(key(saleId, variantId));
        return v == null ? fallback : Math.max(Integer.parseInt(v), 0);
    }

    private static String key(Long saleId, Long variantId) {
        return "fs:" + saleId + ":" + variantId;
    }
}
