package com.datdofishing.catalog.repo;

import com.datdofishing.catalog.domain.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("select v from ProductVariant v join fetch v.product p where v.id in :ids")
    List<ProductVariant> findWithProductByIdIn(Collection<Long> ids);

    boolean existsBySku(String sku);
}
