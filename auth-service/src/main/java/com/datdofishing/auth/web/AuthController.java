package com.datdofishing.auth.web;

import com.datdofishing.auth.service.AuthService;
import com.datdofishing.auth.service.GoogleTokenVerifier;
import com.datdofishing.auth.web.dto.AuthDtos.ForgotPasswordRequest;
import com.datdofishing.auth.web.dto.AuthDtos.GoogleLoginRequest;
import com.datdofishing.auth.web.dto.AuthDtos.LoginRequest;
import com.datdofishing.auth.web.dto.AuthDtos.LogoutRequest;
import com.datdofishing.auth.web.dto.AuthDtos.MessageResponse;
import com.datdofishing.auth.web.dto.AuthDtos.RefreshRequest;
import com.datdofishing.auth.web.dto.AuthDtos.RegisterRequest;
import com.datdofishing.auth.web.dto.AuthDtos.ResetPasswordRequest;
import com.datdofishing.auth.web.dto.AuthDtos.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API đăng nhập cho site cửa hàng (khách hàng). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final GoogleTokenVerifier google;

    public AuthController(AuthService auth, GoogleTokenVerifier google) {
        this.auth = auth;
        this.google = google;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req.email(), req.password());
    }

    @PostMapping("/google")
    public TokenResponse google(@Valid @RequestBody GoogleLoginRequest req) {
        return auth.googleLogin(req.idToken());
    }

    /** Frontend lấy Client ID để hiện nút "Đăng nhập với Google". */
    @GetMapping("/google/config")
    public Map<String, String> googleConfig() {
        return Map.of("clientId", google.clientId() == null ? "" : google.clientId());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestBody(required = false) LogoutRequest req,
                                  @AuthenticationPrincipal Jwt jwt) {
        auth.logout(req == null ? null : req.refreshToken(), jwt);
        return new MessageResponse("Đã đăng xuất");
    }

    @GetMapping("/verify-email")
    public MessageResponse verifyEmail(@RequestParam String token) {
        auth.verifyEmail(token);
        return new MessageResponse("Xác thực email thành công");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        auth.forgotPassword(req.email());
        return new MessageResponse("Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req.token(), req.newPassword());
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }
}
