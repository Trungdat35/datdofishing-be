package com.datdofishing.promotion.repo;

import com.datdofishing.promotion.domain.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, Long> {

    @Query("select i from FlashSaleItem i join fetch i.flashSale f where f.active = true and f.startAt <= :now "
            + "and f.endAt > :now and i.variantId in :variantIds")
    List<FlashSaleItem> findRunningByVariantIds(Instant now, Collection<Long> variantIds);

    Optional<FlashSaleItem> findByFlashSaleIdAndVariantId(Long flashSaleId, Long variantId);
}
