package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.VendorInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorInventoryRepository extends JpaRepository<VendorInventory, Long> {

    @EntityGraph(attributePaths = {"masterProduct", "masterProduct.category"})
    Optional<VendorInventory> findByVendorIdAndMasterProductId(Long vendorId, Long masterProductId);

    @EntityGraph(attributePaths = {"masterProduct", "masterProduct.category"})
    Page<VendorInventory> findByVendorIdAndIsClearedByUserFalse(Long vendorId, Pageable pageable);

    @EntityGraph(attributePaths = {"masterProduct", "masterProduct.category"})
    List<VendorInventory> findByVendorIdAndIsClearedByUserFalse(Long vendorId);

    @Query("SELECT v.masterProduct.id FROM VendorInventory v WHERE v.vendor.id = :vendorId AND v.isClearedByUser = false AND v.currentStock > 0")
    List<Long> findActiveMasterProductIdsByVendor(@Param("vendorId") Long vendorId);

    // Vendor specific valuations excluding banned items
    @Query("SELECT COALESCE(SUM(v.currentStock * m.mrp), 0) " +
            "FROM VendorInventory v JOIN v.masterProduct m " +
            "WHERE v.vendor.id = :vendorId AND v.isClearedByUser = false AND m.isBanned = false")
    BigDecimal calculateValuationAtMrp(@Param("vendorId") Long vendorId);

    @Query("SELECT COALESCE(SUM(v.currentStock * v.sellingPrice), 0) " +
            "FROM VendorInventory v JOIN v.masterProduct m " +
            "WHERE v.vendor.id = :vendorId AND v.isClearedByUser = false AND m.isBanned = false")
    BigDecimal calculateValuationAtSellingPrice(@Param("vendorId") Long vendorId);

    // Platform-wide valuations for Admin overview excluding banned items
    @Query("SELECT COALESCE(SUM(v.currentStock * m.mrp), 0) " +
            "FROM VendorInventory v JOIN v.masterProduct m " +
            "WHERE v.isClearedByUser = false AND m.isBanned = false")
    BigDecimal calculateGlobalValuationAtMrp();

    @Query("SELECT COALESCE(SUM(v.currentStock * v.sellingPrice), 0) " +
            "FROM VendorInventory v JOIN v.masterProduct m " +
            "WHERE v.isClearedByUser = false AND m.isBanned = false")
    BigDecimal calculateGlobalValuationAtSellingPrice();
}