package com.datdofishing.inventory.repo;

import com.datdofishing.inventory.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByOrderIdAndStatus(Long orderId, Reservation.Status status);

    boolean existsByOrderId(Long orderId);
}
