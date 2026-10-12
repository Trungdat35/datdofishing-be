package com.datdofishing.auth.service;

import com.datdofishing.auth.config.AppProperties;
import com.datdofishing.common.error.BusinessException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Xác minh ID token do Google Identity Services cấp cho frontend:
 * kiểm tra chữ ký bằng khóa công khai của Google, issuer, audience = Client ID của mình, hạn dùng.
 */
@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(AppProperties props) {
        this.clientId = props.google() == null ? null : props.google().clientId();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS).build();
        OAuth2TokenValidator<Jwt> issuer = jwt -> ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sai issuer", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Sai audience", null));
        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience));
    }

    public String clientId() {
        return clientId;
    }

    public GoogleUser verify(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw BusinessException.badRequest("GOOGLE_NOT_CONFIGURED",
                    "Chưa cấu hình GOOGLE_CLIENT_ID cho auth-service");
        }
        try {
            Jwt jwt = decoder.decode(idToken);
            Boolean verified = jwt.getClaim("email_verified");
            return new GoogleUser(jwt.getSubject(), jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"), Boolean.TRUE.equals(verified));
        } catch (JwtException e) {
            throw BusinessException.unauthorized("Token Google không hợp lệ: " + e.getMessage());
        }
    }

    public record GoogleUser(String sub, String email, String name, String picture, boolean emailVerified) {
    }
}
