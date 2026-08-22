package com.pankaj.mvm.controller;

import com.pankaj.mvm.dto.AdminReviewProductRequest;
import com.pankaj.mvm.dto.CategoryRequest;
import com.pankaj.mvm.dto.MasterProductRequest;
import com.pankaj.mvm.dto.VendorAddExistingProductRequest;
import com.pankaj.mvm.dto.VendorProposeProductRequest;
import com.pankaj.mvm.entity.Category;
import com.pankaj.mvm.entity.MasterProduct;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.VendorInventory;
import com.pankaj.mvm.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    // --- Category Endpoints ---

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(catalogService.createCategory(request));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(catalogService.getAllCategories());
    }

    @GetMapping("/categories/roots")
    public ResponseEntity<List<Category>> getRootCategories() {
        return ResponseEntity.ok(catalogService.getRootCategories());
    }

    @GetMapping("/categories/{parentId}/subcategories")
    public ResponseEntity<List<Category>> getSubCategories(@PathVariable Long parentId) {
        return ResponseEntity.ok(catalogService.getSubCategories(parentId));
    }

    @PatchMapping("/categories/{categoryId}/ban")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Category> toggleCategoryBan(@PathVariable Long categoryId, @RequestParam boolean ban) {
        return ResponseEntity.ok(catalogService.toggleCategoryBan(categoryId, ban));
    }

    // --- Master Product Endpoints ---

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MasterProduct> createMasterProduct(@Valid @RequestBody MasterProductRequest request) {
        return ResponseEntity.ok(catalogService.createMasterProduct(request));
    }

    @PostMapping("/products/propose")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<MasterProduct> proposeProduct(
            @Valid @RequestBody VendorProposeProductRequest request,
            @AuthenticationPrincipal User vendor) {
        return ResponseEntity.ok(catalogService.proposeProductByVendor(request, vendor));
    }

    @PostMapping("/products/sell-existing")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<VendorInventory> sellExistingProduct(
            @Valid @RequestBody VendorAddExistingProductRequest request,
            @AuthenticationPrincipal User vendor) {
        return ResponseEntity.ok(catalogService.addExistingProductToInventory(request, vendor));
    }

    @PatchMapping("/products/{productId}/review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MasterProduct> reviewProduct(
            @PathVariable Long productId,
            @Valid @RequestBody AdminReviewProductRequest request) {
        MasterProduct reviewed = catalogService.reviewProposedProduct(productId, request);
        return ResponseEntity.ok(reviewed);
    }

    @PatchMapping("/products/{productId}/ban")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MasterProduct> toggleProductBan(@PathVariable Long productId, @RequestParam boolean ban) {
        return ResponseEntity.ok(catalogService.toggleProductBan(productId, ban));
    }

    @GetMapping("/products/active")
    public ResponseEntity<Page<MasterProduct>> getActiveCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal User currentUser) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(catalogService.getActiveCatalog(currentUser, pageable));
    }

    @GetMapping("/products/approval-queue")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Page<MasterProduct>> getApprovalQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(catalogService.getApprovalQueue(pageable));
    }

    @GetMapping("/products/banned")
    public ResponseEntity<Page<MasterProduct>> getBannedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        return ResponseEntity.ok(catalogService.getBannedProducts(pageable));
    }
}