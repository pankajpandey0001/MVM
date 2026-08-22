package com.pankaj.mvm.controller;

import com.pankaj.mvm.dto.StatusChangeRequest;
import com.pankaj.mvm.dto.VendorValuationDashboardResponse;
import com.pankaj.mvm.entity.StockLedger;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.UserAuditLog;
import com.pankaj.mvm.entity.VendorInventory;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import com.pankaj.mvm.service.UserGovernanceService;
import com.pankaj.mvm.service.VendorInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/governance")
@RequiredArgsConstructor
// ALLOW BOTH ROLES HERE
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class SuperAdminGovernanceController {

    private final UserGovernanceService governanceService;
    private final VendorInventoryService vendorInventoryService;

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getTabCounts(@RequestParam(required = false) Role role) {
        if (role != null) {
            return ResponseEntity.ok(governanceService.getStatusTabCountsByRole(role));
        }
        return ResponseEntity.ok(governanceService.getStatusTabCounts());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<User>> getUsers(
            @RequestParam AccountStatus status,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(governanceService.getUsersByStatusAndRole(status, role, pageable));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(governanceService.transitionUserStatus(userId, request));
    }

    @DeleteMapping("/users/{userId}/reject")
    public ResponseEntity<Void> rejectAndPurgeUser(@PathVariable Long userId) {
        governanceService.rejectAndPurgeUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/audit-trail")
    public ResponseEntity<List<UserAuditLog>> getUserAuditTrail(@PathVariable Long userId) {
        return ResponseEntity.ok(governanceService.getUserAuditTrail(userId));
    }

    // ==========================================
    // READ-ONLY VENDOR INVENTORY FOR ADMIN & SUPER ADMIN
    // ==========================================

    @GetMapping("/vendors/{vendorId}/inventory")
    public ResponseEntity<Page<VendorInventory>> getVendorInventoryForAdmin(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(vendorInventoryService.getVendorInventory(vendorId, pageable));
    }

    @GetMapping("/vendors/{vendorId}/valuation")
    public ResponseEntity<VendorValuationDashboardResponse> getVendorValuationForAdmin(@PathVariable Long vendorId) {
        return ResponseEntity.ok(vendorInventoryService.getValuationDashboard(vendorId));
    }

    @GetMapping("/vendors/{vendorId}/ledger")
    public ResponseEntity<Page<StockLedger>> getVendorLedgerForAdmin(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(vendorInventoryService.getVendorLedgerTrail(vendorId, pageable));
    }
}