package com.datdofishing.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    private String phone;
    private String avatarUrl;

    /** Lưu dạng "CUSTOMER,ADMIN" cho gọn. */
    @Column(nullable = false)
    private String roles = Role.CUSTOMER.name();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    private String googleSub;
    private boolean emailVerified;
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    public List<String> roleList() {
        return Arrays.stream(roles.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean hasRole(Role role) {
        return roleList().contains(role.name());
    }

    public void addRole(Role role) {
        Set<String> set = new LinkedHashSet<>(roleList());
        set.add(role.name());
        roles = set.stream().collect(Collectors.joining(","));
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
