package com.datdofishing.content.repo;

import com.datdofishing.content.domain.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByPositionAndActiveTrueOrderBySortOrderAsc(String position);

    List<Banner> findAllByOrderByPositionAscSortOrderAsc();
}
