package com.datdofishing.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Lớp bảo vệ đầu tiên: kiểm tra chữ ký JWT bằng JWKS của auth-service, chặn token đã đăng xuất,
 * chặn /api/admin/** nếu không có quyền ADMIN. Các service phía sau vẫn tự kiểm tra lại.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC = {
            "/api/auth/register", "/api/auth/login", "/api/auth/google", "/api/auth/google/config",
            "/api/auth/refresh", "/api/auth/verify-email", "/api/auth/forgot-password", "/api/auth/reset-password",
            "/api/admin/auth/login", "/.well-known/**", "/actuator/health/**", "/actuator/health"
    };

    private static final String[] PUBLIC_GET = {
            "/api/catalog/**", "/api/inventory/**", "/api/promotions/**", "/api/content/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder decoder) {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(PUBLIC).permitAll()
                        .pathMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j
                        .jwtDecoder(decoder)
                        .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(converter))))
                .build();
    }

    /** Giải mã JWT bằng JWKS, sau đó kiểm tra danh sách token đã đăng xuất trong Redis (bl:{jti}). */
    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${datdofishing.jwks-uri}") String jwksUri,
                                         ReactiveStringRedisTemplate redis) {
        NimbusReactiveJwtDecoder delegate = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build();
        return token -> delegate.decode(token).flatMap(jwt -> {
            if (jwt.getId() == null) {
                return Mono.just(jwt);
            }
            return redis.hasKey("bl:" + jwt.getId()).flatMap(revoked -> Boolean.TRUE.equals(revoked)
                    ? Mono.error(new BadJwtException("Token đã đăng xuất"))
                    : Mono.just(jwt));
        });
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${datdofishing.cors.allowed-origins}") List<String> origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("X-Instance-Id", "Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
