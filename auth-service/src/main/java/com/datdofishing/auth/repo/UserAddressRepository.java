package com.datdofishing.auth.repo;

import com.datdofishing.auth.domain.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIdDesc(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);
}
