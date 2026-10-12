package com.datdofishing.inventory.repo;

import com.datdofishing.inventory.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /** Khóa các dòng tồn kho theo thứ tự variant_id để tránh deadlock khi nhiều đơn cùng lúc. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.variantId in :ids order by s.variantId")
    List<Stock> lockByVariantIds(Collection<Long> ids);
}
