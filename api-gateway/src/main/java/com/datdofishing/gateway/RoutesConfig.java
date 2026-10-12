package com.datdofishing.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Bảng định tuyến. /internal/** không được định tuyến nên bên ngoài không gọi được.
 * order-service dùng "lb://" để Spring Cloud LoadBalancer chia tải giữa các instance.
 */
@Configuration
public class RoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RedisRateLimiter loginRateLimiter,
                               KeyResolver ipKeyResolver,
                               @Value("${datdofishing.routes.auth}") String auth,
                               @Value("${datdofishing.routes.catalog}") String catalog,
                               @Value("${datdofishing.routes.inventory}") String inventory,
                               @Value("${datdofishing.routes.cart}") String cart,
                               @Value("${datdofishing.routes.order}") String order,
                               @Value("${datdofishing.routes.promotion}") String promotion,
                               @Value("${datdofishing.routes.content}") String content,
                               @Value("${datdofishing.routes.notification}") String notification) {
        return builder.routes()
                // Đăng nhập: giới hạn tần suất theo IP để chống dò mật khẩu
                .route("auth-login", r -> r.path("/api/auth/login", "/api/admin/auth/login", "/api/auth/register",
                                "/api/auth/forgot-password")
                        .filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(loginRateLimiter)
                                .setKeyResolver(ipKeyResolver)))
                        .uri(auth))
                .route("auth", r -> r.path("/api/auth/**", "/api/users/**", "/api/admin/auth/**",
                        "/api/admin/users/**", "/.well-known/**").uri(auth))
                .route("catalog", r -> r.path("/api/catalog/**", "/api/admin/catalog/**").uri(catalog))
                .route("inventory", r -> r.path("/api/inventory/**", "/api/admin/inventory/**").uri(inventory))
                .route("cart", r -> r.path("/api/cart/**").uri(cart))
                .route("order", r -> r.path("/api/orders/**", "/api/admin/orders/**").uri(order))
                .route("promotion", r -> r.path("/api/promotions/**", "/api/admin/promotions/**").uri(promotion))
                .route("content", r -> r.path("/api/content/**", "/api/admin/content/**").uri(content))
                .route("notification", r -> r.path("/api/notifications/**", "/api/admin/notifications/**")
                        .uri(notification))
                .build();
    }

    /** 5 request/giây, tối đa dồn 10 request mỗi IP cho các API đăng nhập. */
    @Bean
    @Primary
    public RedisRateLimiter loginRateLimiter() {
        return new RedisRateLimiter(5, 10);
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return Mono.just(forwarded.split(",")[0].trim());
            }
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            return Mono.just(remote == null ? "unknown" : remote.getAddress().getHostAddress());
        };
    }
}
