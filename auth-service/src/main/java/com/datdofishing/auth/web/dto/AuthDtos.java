package com.datdofishing.auth.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Các DTO cho API đăng nhập / người dùng. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự") String password,
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
            @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ") String phone) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record GoogleLoginRequest(@NotBlank(message = "Thiếu idToken") String idToken) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    public record ResetPasswordRequest(@NotBlank String token,
                                       @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
                                       String newPassword) {
    }

    public record ChangePasswordRequest(String currentPassword,
                                        @NotBlank @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
                                        String newPassword) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                                UserResponse user) {
    }

    public record UserResponse(Long id, String email, String fullName, String phone, String avatarUrl,
                               List<String> roles, String provider, boolean emailVerified, boolean enabled,
                               Instant createdAt) {
    }

    public record UpdateProfileRequest(@NotBlank @Size(max = 150) String fullName,
                                       @Pattern(regexp = "^$|^0\\d{9,10}$", message = "Số điện thoại không hợp lệ")
                                       String phone,
                                       @Size(max = 500) String avatarUrl) {
    }

    public record AddressRequest(@NotBlank String recipientName,
                                 @NotBlank @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ")
                                 String phone,
                                 @NotBlank String province, @NotBlank String district, @NotBlank String ward,
                                 @NotBlank String street, boolean defaultAddress) {
    }

    public record AddressResponse(Long id, String recipientName, String phone, String province, String district,
                                  String ward, String street, boolean defaultAddress) {
    }

    public record UpdateUserStatusRequest(boolean enabled) {
    }

    public record MessageResponse(String message) {
    }
}
