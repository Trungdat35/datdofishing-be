package com.datdofishing.auth.service;

import com.datdofishing.auth.domain.User;
import com.datdofishing.auth.domain.UserAddress;
import com.datdofishing.auth.repo.UserAddressRepository;
import com.datdofishing.auth.repo.UserRepository;
import com.datdofishing.auth.web.dto.AuthDtos.AddressRequest;
import com.datdofishing.auth.web.dto.AuthDtos.AddressResponse;
import com.datdofishing.auth.web.dto.AuthDtos.ChangePasswordRequest;
import com.datdofishing.auth.web.dto.AuthDtos.UpdateProfileRequest;
import com.datdofishing.auth.web.dto.AuthDtos.UserResponse;
import com.datdofishing.common.error.BusinessException;
import com.datdofishing.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserService {

    private final UserRepository users;
    private final UserAddressRepository addresses;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, UserAddressRepository addresses, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.addresses = addresses;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return AuthService.toUser(get(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User u = get(userId);
        u.setFullName(req.fullName().trim());
        u.setPhone(StringUtils.hasText(req.phone()) ? req.phone() : null);
        u.setAvatarUrl(StringUtils.hasText(req.avatarUrl()) ? req.avatarUrl() : u.getAvatarUrl());
        return AuthService.toUser(u);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User u = get(userId);
        // Tài khoản Google chưa có mật khẩu thì được đặt mới không cần mật khẩu cũ
        if (u.getPasswordHash() != null
                && (req.currentPassword() == null || !passwordEncoder.matches(req.currentPassword(), u.getPasswordHash()))) {
            throw BusinessException.badRequest("WRONG_PASSWORD", "Mật khẩu hiện tại không đúng");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> addresses(Long userId) {
        return addresses.findByUserIdOrderByIdDesc(userId).stream().map(UserService::toAddress).toList();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest req) {
        UserAddress a = new UserAddress();
        a.setUserId(userId);
        boolean first = addresses.findByUserIdOrderByIdDesc(userId).isEmpty();
        apply(a, req, userId, first);
        return toAddress(addresses.save(a));
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long id, AddressRequest req) {
        UserAddress a = addresses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("Địa chỉ"));
        apply(a, req, userId, false);
        return toAddress(a);
    }

    @Transactional
    public void deleteAddress(Long userId, Long id) {
        UserAddress a = addresses.findByIdAndUserId(id, userId)
                .orElseThrow(() -> BusinessException.notFound("Địa chỉ"));
        addresses.delete(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, int page, int size) {
        return PageResponse.of(users.search(q == null ? "" : q.trim(),
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"))), AuthService::toUser);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User u = get(id);
        u.setEnabled(enabled);
        return AuthService.toUser(u);
    }

    private void apply(UserAddress a, AddressRequest req, Long userId, boolean forceDefault) {
        if (req.defaultAddress() || forceDefault) {
            for (UserAddress other : addresses.findByUserIdOrderByIdDesc(userId)) {
                other.setDefaultAddress(false);
            }
            a.setDefaultAddress(true);
        }
        a.setRecipientName(req.recipientName());
        a.setPhone(req.phone());
        a.setProvince(req.province());
        a.setDistrict(req.district());
        a.setWard(req.ward());
        a.setStreet(req.street());
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> BusinessException.notFound("Tài khoản"));
    }

    private static AddressResponse toAddress(UserAddress a) {
        return new AddressResponse(a.getId(), a.getRecipientName(), a.getPhone(), a.getProvince(), a.getDistrict(),
                a.getWard(), a.getStreet(), a.isDefaultAddress());
    }
}
