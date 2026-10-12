package com.datdofishing.promotion.repo;

import com.datdofishing.promotion.domain.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface FlashSaleRepository extends JpaRepository<FlashSale, Long> {

    /** Flash sale đang chạy hoặc sắp chạy gần nhất. */
    @Query("select f from FlashSale f where f.active = true and f.endAt > :now order by f.startAt asc")
    List<FlashSale> findCurrentAndUpcoming(Instant now);

    List<FlashSale> findAllByOrderByStartAtDesc();
}
