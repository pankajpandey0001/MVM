package com.pankaj.mvm.repository;

import com.pankaj.mvm.entity.StockLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, Long> {

    @EntityGraph(attributePaths = {
            "vendorInventory",
            "vendorInventory.masterProduct",
            "vendorInventory.vendor"
    })
    Page<StockLedger> findByVendorInventoryVendorIdOrderByTimestampDesc(Long vendorId, Pageable pageable);
}