package com.datdofishing.auth.config;

import com.datdofishing.auth.domain.Role;
import com.datdofishing.auth.domain.User;
import com.datdofishing.auth.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Tạo tài khoản admin đầu tiên nếu chưa có. */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminSeeder(UserRepository users, PasswordEncoder passwordEncoder, AppProperties props) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        if (admin == null || admin.email() == null || users.existsByEmailIgnoreCase(admin.email())) {
            return;
        }
        User u = new User();
        u.setEmail(admin.email().toLowerCase());
        u.setPasswordHash(passwordEncoder.encode(admin.password()));
        u.setFullName(admin.fullName() == null ? "Admin" : admin.fullName());
        u.setEmailVerified(true);
        u.addRole(Role.ADMIN);
        users.save(u);
        log.info("Đã tạo tài khoản admin mặc định: {}", admin.email());
    }
}
