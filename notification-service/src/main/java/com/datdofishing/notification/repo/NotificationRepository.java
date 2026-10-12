package com.datdofishing.notification.repo;

import com.datdofishing.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByAudienceAndUserIdOrderByCreatedAtDesc(String audience, Long userId, Pageable pageable);

    Page<Notification> findByAudienceOrderByCreatedAtDesc(String audience, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByAudienceAndUserIdAndReadFalse(String audience, Long userId);

    @Modifying
    @Query("update Notification n set n.read = true where n.audience = 'USER' and n.userId = :userId and n.read = false")
    int markAllRead(Long userId);
}
