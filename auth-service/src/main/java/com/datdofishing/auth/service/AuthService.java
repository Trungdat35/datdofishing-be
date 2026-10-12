package com.datdofishing.auth.service;

import com.datdofishing.auth.domain.AuthProvider;
import com.datdofishing.auth.domain.Role;
import com.datdofishing.auth.domain.User;
import com.datdofishing.auth.repo.UserRepository;
import com.datdofishing.auth.web.dto.AuthDtos.RegisterRequest;
import com.datdofishing.auth.web.dto.AuthDtos.TokenResponse;
import com.datdofishing.auth.web.dto.AuthDtos.UserResponse;
import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.event.Topics;
import com.datdofishing.common.outbox.OutboxWriter;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    static final String VERIFY = "verify";
    static final String RESET = "reset";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final GoogleTokenVerifier googleVerifier;
    private final OutboxWriter outbox;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
                       GoogleTokenVerifier googleVerifier, OutboxWriter outbox) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.googleVerifier = googleVerifier;
        this.outbox = outbox;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("EMAIL_EXISTS", "Email đã được đăng ký");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName().trim());
        user.setPhone(StringUtils.hasText(req.phone()) ? req.phone() : null);
        user.setProvider(AuthProvider.LOCAL);
        users.save(user);
        publishRegistered(user);
        return toTokenResponse(user, TokenService.AUD_SHOP);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> BusinessException.unauthorized("Email hoặc mật khẩu không đúng"));
        ensureEnabled(user);
        return toTokenResponse(user, TokenService.AUD_SHOP);
    }

    @Transactional(readOnly = true)
    public TokenResponse adminLogin(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> BusinessException.unauthorized("Email hoặc mật khẩu không đúng"));
        ensureEnabled(user);
        if (!user.hasRole(Role.ADMIN)) {
            throw BusinessException.forbidden("Tài khoản không có quyền quản trị");
        }
        return toTokenResponse(user, TokenService.AUD_ADMIN);
    }

    @Transactional
    public TokenResponse googleLogin(String idToken) {
        GoogleTokenVerifier.GoogleUser g = googleVerifier.verify(idToken);
        if (!g.emailVerified()) {
            throw BusinessException.unauthorized("Email Google chưa được xác minh");
        }
        User user = users.findByGoogleSub(g.sub())
                .or(() -> users.findByEmailIgnoreCase(g.email()))
                .orElse(null);
        boolean created = false;
        if (user == null) {
            user = new User();
            user.setEmail(g.email().toLowerCase());
            user.setFullName(StringUtils.hasText(g.name()) ? g.name() : g.email());
            user.setProvider(AuthProvider.GOOGLE);
            created = true;
        }
        // Liên kết tài khoản email cũ với Google nếu chưa liên kết
        user.setGoogleSub(g.sub());
        user.setEmailVerified(true);
        if (user.getAvatarUrl() == null) {
            user.setAvatarUrl(g.picture());
        }
        users.save(user);
        ensureEnabled(user);
        if (created) {
            publishRegistered(user);
        }
        return toTokenResponse(user, TokenService.AUD_SHOP);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        String[] parts = tokens.consumeRefreshToken(refreshToken);
        User user = users.findById(Long.valueOf(parts[0]))
                .orElseThrow(() -> BusinessException.unauthorized("Tài khoản không tồn tại"));
        ensureEnabled(user);
        return toTokenResponse(user, parts[1]);
    }

    public void logout(String refreshToken, Jwt accessToken) {
        tokens.revoke(refreshToken, accessToken);
    }

    @Transactional
    public void verifyEmail(String token) {
        Long userId = tokens.consumeOneTimeToken(VERIFY, token);
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("Tài khoản"));
        user.setEmailVerified(true);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Luôn trả về thành công để không lộ email nào đã đăng ký
        users.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String token = tokens.createOneTimeToken(RESET, user.getId(), Duration.ofMinutes(30));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", user.getId());
            payload.put("email", user.getEmail());
            payload.put("fullName", user.getFullName());
            payload.put("resetToken", token);
            outbox.write(Topics.USER_PASSWORD_RESET, String.valueOf(user.getId()), "PasswordResetRequested", payload);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Long userId = tokens.consumeOneTimeToken(RESET, token);
        User user = users.findById(userId).orElseThrow(() -> BusinessException.notFound("Tài khoản"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    private void publishRegistered(User user) {
        String verifyToken = user.isEmailVerified() ? null
                : tokens.createOneTimeToken(VERIFY, user.getId(), Duration.ofHours(24));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId());
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getFullName());
        payload.put("phone", user.getPhone());
        payload.put("provider", user.getProvider().name());
        payload.put("verifyToken", verifyToken);
        payload.put("registeredAt", user.getCreatedAt());
        outbox.write(Topics.USER_REGISTERED, String.valueOf(user.getId()), "UserRegistered", payload);
    }

    private void ensureEnabled(User user) {
        if (!user.isEnabled()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị khóa");
        }
    }

    private TokenResponse toTokenResponse(User user, String audience) {
        TokenService.TokenPair pair = tokens.issue(user, audience);
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresIn(), toUser(user));
    }

    public static UserResponse toUser(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getAvatarUrl(),
                u.roleList(), u.getProvider().name(), u.isEmailVerified(), u.isEnabled(), u.getCreatedAt());
    }
}
