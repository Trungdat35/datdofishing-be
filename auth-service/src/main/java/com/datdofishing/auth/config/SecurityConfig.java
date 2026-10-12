package com.datdofishing.auth.config;

import com.datdofishing.common.security.ResourceServerSecurity;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, AppProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return ResourceServerSecurity.build(http,
                new String[]{"/api/auth/verify-email", "/api/auth/google/config", "/google-test.html"},
                new String[]{"/api/auth/register", "/api/auth/login", "/api/auth/google", "/api/auth/refresh",
                        "/api/auth/forgot-password", "/api/auth/reset-password", "/api/admin/auth/login",
                        "/.well-known/**"});
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(SigningKeyProvider keys) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys.rsaKey())));
    }

    /** auth-service tự kiểm tra token của chính nó bằng public key, không cần gọi JWKS qua mạng. */
    @Bean
    public JwtDecoder jwtDecoder(SigningKeyProvider keys) {
        return NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
    }
}
