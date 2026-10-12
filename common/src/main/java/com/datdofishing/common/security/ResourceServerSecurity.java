package com.datdofishing.common.security;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình chung cho các service đóng vai trò resource server:
 * stateless, kiểm tra JWT bằng JWKS của auth-service, /api/admin/** cần quyền ADMIN.
 */
public final class ResourceServerSecurity {

    private ResourceServerSecurity() {
    }

    /**
     * @param publicGetPaths các đường dẫn GET không cần đăng nhập
     * @param publicPaths    các đường dẫn mọi method không cần đăng nhập
     */
    public static SecurityFilterChain build(HttpSecurity http, String[] publicGetPaths, String[] publicPaths)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry r = auth;
                    // Cho phép lượt dispatch ASYNC/ERROR (SSE kết thúc, trang lỗi) không bị chặn lần 2
                    r.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll();
                    r.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                            "/internal/**", "/error").permitAll();
                    if (publicGetPaths.length > 0) {
                        r.requestMatchers(HttpMethod.GET, publicGetPaths).permitAll();
                    }
                    if (publicPaths.length > 0) {
                        r.requestMatchers(publicPaths).permitAll();
                    }
                    r.requestMatchers("/api/admin/**").hasRole("ADMIN");
                    r.anyRequest().authenticated();
                })
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(new JwtRoleConverter())));
        return http.build();
    }
}
