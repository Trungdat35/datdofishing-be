package com.datdofishing.promotion.repo;

import com.datdofishing.promotion.domain.FlashSaleClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashSaleClaimRepository extends JpaRepository<FlashSaleClaim, Long> {
    List<FlashSaleClaim> findByOrderCodeAndStatus(String orderCode, String status);
}
