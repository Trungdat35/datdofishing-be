package com.datdofishing.catalog.repo;

import com.datdofishing.catalog.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Brand> findAllByOrderByNameAsc();
}
