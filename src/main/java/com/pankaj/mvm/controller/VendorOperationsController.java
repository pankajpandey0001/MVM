package com.pankaj.mvm.controller;

import com.pankaj.mvm.dto.StockAdjustmentRequest;
import com.pankaj.mvm.dto.UpdateSellingPriceRequest;
import com.pankaj.mvm.dto.VendorValuationDashboardResponse;
import com.pankaj.mvm.entity.StockLedger;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.VendorInventory;
import com.pankaj.mvm.service.VendorInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vendor/operations")
@RequiredArgsConstructor
public class VendorOperationsController {

    private final VendorInventoryService vendorInventoryService;

    // --- Vendor Self-Service Endpoints ---

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorValuationDashboardResponse> getDashboard(@AuthenticationPrincipal User vendor) {
        return ResponseEntity.ok(vendorInventoryService.getValuationDashboard(vendor.getId()));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Page<VendorInventory>> getInventory(
            @AuthenticationPrincipal User vendor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return ResponseEntity.ok(vendorInventoryService.getVendorInventory(vendor.getId(), pageable));
    }

    @PostMapping("/stock/adjust")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<StockLedger> adjustStock(
            @AuthenticationPrincipal User vendor,
            @Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(vendorInventoryService.adjustStock(vendor, request));
    }

    @PatchMapping("/price/update")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorInventory> updatePrice(
            @AuthenticationPrincipal User vendor,
            @Valid @RequestBody UpdateSellingPriceRequest request) {
        return ResponseEntity.ok(vendorInventoryService.updateSellingPrice(vendor, request));
    }

    @DeleteMapping("/banned-products/{productId}/clear")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Void> clearBannedProduct(
            @AuthenticationPrincipal User vendor,
            @PathVariable Long productId) {
        vendorInventoryService.clearBannedProductFromStore(vendor, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ledger/trail")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<Page<StockLedger>> getLedgerTrail(
            @AuthenticationPrincipal User vendor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(vendorInventoryService.getVendorLedgerTrail(vendor.getId(), pageable));
    }

    // --- Admin & Super Admin Read-Only Inspection Endpoints ---

    @GetMapping("/admin/vendor/{vendorId}/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<VendorValuationDashboardResponse> getVendorDashboardForAdmin(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorInventoryService.getValuationDashboard(vendorId));
    }

    @GetMapping("/admin/platform-valuation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<VendorValuationDashboardResponse> getGlobalPlatformValuation() {
        return ResponseEntity.ok(vendorInventoryService.getGlobalPlatformValuation());
    }

    @GetMapping("/admin/vendor/{vendorId}/inventory")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<VendorInventory>> getVendorInventoryForAdmin(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return ResponseEntity.ok(vendorInventoryService.getVendorInventory(vendorId, pageable));
    }
}