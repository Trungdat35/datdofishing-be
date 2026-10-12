package com.datdofishing.auth.service;

import com.datdofishing.auth.config.JwtProperties;
import com.datdofishing.auth.domain.User;
import com.datdofishing.common.error.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Cấp access token (JWT RS256) và refresh token (chuỗi ngẫu nhiên lưu Redis, đổi mới mỗi lần dùng).
 * Key Redis:
 *   rt:{token}  -> "{userId}|{audience}"  (sống refresh-ttl)
 *   bl:{jti}    -> "1"                    (access token đã đăng xuất, sống tới khi token hết hạn)
 */
@Service
public class TokenService {

    public static final String AUD_SHOP = "shop";
    public static final String AUD_ADMIN = "admin";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final StringRedisTemplate redis;

    public TokenService(JwtEncoder encoder, JwtProperties props, StringRedisTemplate redis) {
        this.encoder = encoder;
        this.props = props;
        this.redis = redis;
    }

    public TokenPair issue(User user, String audience) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(String.valueOf(user.getId()))
                .audience(java.util.List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTtl()))
                .id(UUID.randomUUID().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("roles", user.roleList())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String access = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        String refresh = randomToken();
        redis.opsForValue().set("rt:" + refresh, user.getId() + "|" + audience, props.refreshTtl());
        return new TokenPair(access, refresh, props.accessTtl().toSeconds(), audience);
    }

    /** Dùng refresh token một lần: xóa token cũ và trả về userId + audience. */
    public String[] consumeRefreshToken(String refreshToken) {
        String value = redis.opsForValue().getAndDelete("rt:" + refreshToken);
        if (value == null) {
            throw BusinessException.unauthorized("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }
        return value.split("\\|");
    }

    public void revoke(String refreshToken, Jwt accessToken) {
        if (refreshToken != null) {
            redis.delete("rt:" + refreshToken);
        }
        if (accessToken != null && accessToken.getExpiresAt() != null) {
            Duration left = Duration.between(Instant.now(), accessToken.getExpiresAt());
            if (!left.isNegative() && !left.isZero()) {
                redis.opsForValue().set("bl:" + accessToken.getId(), "1", left);
            }
        }
    }

    /** Token dùng 1 lần cho xác thực email / đặt lại mật khẩu. */
    public String createOneTimeToken(String purpose, Long userId, Duration ttl) {
        String token = randomToken();
        redis.opsForValue().set(purpose + ":" + token, String.valueOf(userId), ttl);
        return token;
    }

    public Long consumeOneTimeToken(String purpose, String token) {
        String value = redis.opsForValue().getAndDelete(purpose + ":" + token);
        if (value == null) {
            throw BusinessException.badRequest("TOKEN_INVALID", "Liên kết không hợp lệ hoặc đã hết hạn");
        }
        return Long.valueOf(value);
    }

    private static String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn, String audience) {
    }
}
