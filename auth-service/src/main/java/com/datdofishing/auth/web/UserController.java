package com.datdofishing.auth.web;

import com.datdofishing.auth.service.UserService;
import com.datdofishing.auth.web.dto.AuthDtos.AddressRequest;
import com.datdofishing.auth.web.dto.AuthDtos.AddressResponse;
import com.datdofishing.auth.web.dto.AuthDtos.ChangePasswordRequest;
import com.datdofishing.auth.web.dto.AuthDtos.MessageResponse;
import com.datdofishing.auth.web.dto.AuthDtos.UpdateProfileRequest;
import com.datdofishing.auth.web.dto.AuthDtos.UserResponse;
import com.datdofishing.common.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Hồ sơ và sổ địa chỉ của người dùng đang đăng nhập. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse me() {
        return users.me(CurrentUser.id());
    }

    @PutMapping
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest req) {
        return users.updateProfile(CurrentUser.id(), req);
    }

    @PostMapping("/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        users.changePassword(CurrentUser.id(), req);
        return new MessageResponse("Đã đổi mật khẩu");
    }

    @GetMapping("/addresses")
    public List<AddressResponse> addresses() {
        return users.addresses(CurrentUser.id());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(@Valid @RequestBody AddressRequest req) {
        return users.addAddress(CurrentUser.id(), req);
    }

    @PutMapping("/addresses/{id}")
    public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        return users.updateAddress(CurrentUser.id(), id, req);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id) {
        users.deleteAddress(CurrentUser.id(), id);
    }
}
