package com.datdofishing.common.security;

import com.datdofishing.common.error.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/** Lấy thông tin người dùng hiện tại từ JWT. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static JwtAuthenticationToken token() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt;
        }
        throw BusinessException.unauthorized("Bạn cần đăng nhập");
    }

    public static Long id() {
        return Long.valueOf(token().getToken().getSubject());
    }

    public static String email() {
        return token().getToken().getClaimAsString("email");
    }

    public static String fullName() {
        return token().getToken().getClaimAsString("name");
    }

    public static boolean isAdmin() {
        List<String> roles = token().getToken().getClaimAsStringList("roles");
        return roles != null && roles.contains("ADMIN");
    }
}
