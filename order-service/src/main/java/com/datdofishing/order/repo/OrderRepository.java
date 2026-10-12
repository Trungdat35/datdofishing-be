package com.datdofishing.order.repo;

import com.datdofishing.order.domain.Order;
import com.datdofishing.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByCode(String code);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    List<Order> findTop100ByStatusAndCreatedAtBefore(OrderStatus status, Instant before);

    @Query(value = "SELECT status, count(*), coalesce(sum(total), 0) FROM orders GROUP BY status", nativeQuery = true)
    List<Object[]> statsByStatus();
}
