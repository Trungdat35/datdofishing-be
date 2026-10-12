package com.datdofishing.auth.web;

import com.datdofishing.auth.service.AuthService;
import com.datdofishing.auth.service.UserService;
import com.datdofishing.auth.web.dto.AuthDtos.LoginRequest;
import com.datdofishing.auth.web.dto.AuthDtos.TokenResponse;
import com.datdofishing.auth.web.dto.AuthDtos.UpdateUserStatusRequest;
import com.datdofishing.auth.web.dto.AuthDtos.UserResponse;
import com.datdofishing.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API cho site admin: đăng nhập riêng (chỉ tài khoản ADMIN) và quản lý người dùng. */
@RestController
public class AdminAuthController {

    private final AuthService auth;
    private final UserService users;

    public AdminAuthController(AuthService auth, UserService users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/api/admin/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.adminLogin(req.email(), req.password());
    }

    @GetMapping("/api/admin/users")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return users.search(q, page, size);
    }

    @GetMapping("/api/admin/users/{id}")
    public UserResponse get(@PathVariable Long id) {
        return users.me(id);
    }

    @PatchMapping("/api/admin/users/{id}/status")
    public UserResponse setStatus(@PathVariable Long id, @RequestBody UpdateUserStatusRequest req) {
        return users.setEnabled(id, req.enabled());
    }
}
