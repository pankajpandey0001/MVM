package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.MasterProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MasterProductRepository extends JpaRepository<MasterProduct, Long> {

    boolean existsBySku(String sku);

    boolean existsByTitleIgnoreCase(String title);

    List<MasterProduct> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = {"category"})
    Page<MasterProduct> findByIsApprovedTrueAndIsBannedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT m FROM MasterProduct m WHERE m.isApproved = true AND m.isBanned = false AND m.id NOT IN :excludedIds")
    Page<MasterProduct> findAvailableForVendor(@Param("excludedIds") List<Long> excludedIds, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "proposedByVendor"})
    Page<MasterProduct> findByIsApprovedFalse(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<MasterProduct> findByIsBannedTrue(Pageable pageable);
}