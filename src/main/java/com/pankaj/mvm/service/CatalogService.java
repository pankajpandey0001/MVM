package com.pankaj.mvm.service;

import com.pankaj.mvm.dto.AdminReviewProductRequest;
import com.pankaj.mvm.dto.CategoryRequest;
import com.pankaj.mvm.dto.MasterProductRequest;
import com.pankaj.mvm.dto.VendorAddExistingProductRequest;
import com.pankaj.mvm.dto.VendorProposeProductRequest;
import com.pankaj.mvm.entity.Category;
import com.pankaj.mvm.entity.MasterProduct;
import com.pankaj.mvm.entity.StockLedger;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.VendorInventory;
import com.pankaj.mvm.enums.LedgerEntryType;
import com.pankaj.mvm.exceptions.ApiException;
import com.pankaj.mvm.repository.CategoryRepository;
import com.pankaj.mvm.repository.MasterProductRepository;
import com.pankaj.mvm.repository.StockLedgerRepository;
import com.pankaj.mvm.repository.VendorInventoryRepository;
import com.pankaj.mvm.util.SkuGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final MasterProductRepository masterProductRepository;
    private final VendorInventoryRepository vendorInventoryRepository;
    private final StockLedgerRepository stockLedgerRepository;

    // --- Category Management ---

    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new ApiException("Category code already exists: " + request.getCode(), HttpStatus.CONFLICT);
        }

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ApiException("Parent category not found", HttpStatus.NOT_FOUND));
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .parentCategory(parent)
                .isBanned(false)
                .build();

        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAllWithParent();
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentCategoryIsNullAndIsBannedFalse();
    }

    public List<Category> getSubCategories(Long parentId) {
        return categoryRepository.findByParentCategoryIdAndIsBannedFalse(parentId);
    }

    @Transactional
    public Category toggleCategoryBan(Long categoryId, boolean banStatus) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        cascadeCategoryBan(category, banStatus);
        return categoryRepository.save(category);
    }

    private void cascadeCategoryBan(Category category, boolean banStatus) {
        category.setIsBanned(banStatus);

        List<MasterProduct> products = masterProductRepository.findByCategoryId(category.getId());
        for (MasterProduct product : products) {
            product.setIsBanned(banStatus);
            masterProductRepository.save(product);
        }

        if (category.getSubCategories() != null) {
            for (Category sub : category.getSubCategories()) {
                cascadeCategoryBan(sub, banStatus);
            }
        }
    }

    // --- Master Product Catalog & Lifecycle ---

    @Transactional
    public MasterProduct createMasterProduct(MasterProductRequest request) {
        if (masterProductRepository.existsByTitleIgnoreCase(request.getTitle().trim())) {
            throw new ApiException("A product with this title already exists in the catalog", HttpStatus.CONFLICT);
        }

        if (request.getMinPrice().compareTo(request.getMrp()) > 0) {
            throw new ApiException("Admin Minimum Price cannot exceed Admin MRP", HttpStatus.BAD_REQUEST);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(category.getIsBanned())) {
            throw new ApiException("Cannot create product in a banned category", HttpStatus.BAD_REQUEST);
        }

        String sku = (request.getSku() == null || request.getSku().isBlank())
                ? SkuGenerator.generate(category.getCode())
                : request.getSku().trim().toUpperCase();

        if (masterProductRepository.existsBySku(sku)) {
            throw new ApiException("SKU code already exists: " + sku, HttpStatus.CONFLICT);
        }

        MasterProduct product = MasterProduct.builder()
                .sku(sku)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(category)
                .minPrice(request.getMinPrice())
                .mrp(request.getMrp())
                .isBanned(false)
                .isApproved(true)
                .build();

        return masterProductRepository.save(product);
    }

    @Transactional
    public MasterProduct proposeProductByVendor(VendorProposeProductRequest request, User vendor) {
        if (masterProductRepository.existsByTitleIgnoreCase(request.getTitle().trim())) {
            throw new ApiException("A product with this title already exists in the catalog. You can sell the existing product directly.", HttpStatus.CONFLICT);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(category.getIsBanned())) {
            throw new ApiException("Cannot propose a product under a banned category", HttpStatus.BAD_REQUEST);
        }

        String sku = SkuGenerator.generate(category.getCode());

        MasterProduct product = MasterProduct.builder()
                .sku(sku)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(category)
                .minPrice(null)
                .mrp(null)
                .isBanned(false)
                .isApproved(false)
                .proposedByVendor(vendor)
                .build();

        return masterProductRepository.save(product);
    }

    @Transactional
    public MasterProduct reviewProposedProduct(Long productId, AdminReviewProductRequest request) {
        MasterProduct product = masterProductRepository.findById(productId)
                .orElseThrow(() -> new ApiException("Product blueprint not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(request.getApprove())) {
            if (request.getMinPrice() == null || request.getMrp() == null) {
                throw new ApiException("Admin must define both Minimum Price and MRP to approve a product.", HttpStatus.BAD_REQUEST);
            }

            if (request.getMinPrice().compareTo(request.getMrp()) > 0) {
                throw new ApiException("Minimum Price cannot exceed MRP", HttpStatus.BAD_REQUEST);
            }

            if (request.getCategoryId() != null) {
                Category category = categoryRepository.findById(request.getCategoryId())
                        .orElseThrow(() -> new ApiException("Selected category not found", HttpStatus.NOT_FOUND));
                product.setCategory(category);
            }

            product.setMinPrice(request.getMinPrice());
            product.setMrp(request.getMrp());
            product.setIsApproved(true);
            product.setIsBanned(false);

            return masterProductRepository.save(product);
        } else {
            // Hard Delete: Physically remove the rejected proposal from the database
            masterProductRepository.delete(product);
            return null;
        }
    }

    @Transactional
    public VendorInventory addExistingProductToInventory(VendorAddExistingProductRequest request, User vendor) {
        MasterProduct masterProduct = masterProductRepository.findById(request.getMasterProductId())
                .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(masterProduct.getIsApproved()) || Boolean.TRUE.equals(masterProduct.getIsBanned())) {
            throw new ApiException("This product is not active or approved for sale.", HttpStatus.BAD_REQUEST);
        }

        if (masterProduct.getMinPrice() == null || masterProduct.getMrp() == null) {
            throw new ApiException("Price corridor has not been configured by the admin yet.", HttpStatus.BAD_REQUEST);
        }

        if (request.getSellingPrice().compareTo(masterProduct.getMinPrice()) < 0 ||
                request.getSellingPrice().compareTo(masterProduct.getMrp()) > 0) {
            throw new ApiException(
                    String.format("Selling price must fall within the corridor: ₹%s to ₹%s",
                            masterProduct.getMinPrice(), masterProduct.getMrp()),
                    HttpStatus.BAD_REQUEST
            );
        }

        Optional<VendorInventory> existingInventoryOpt =
                vendorInventoryRepository.findByVendorIdAndMasterProductId(vendor.getId(), masterProduct.getId());

        VendorInventory inventory;
        if (existingInventoryOpt.isPresent()) {
            inventory = existingInventoryOpt.get();
            inventory.setSellingPrice(request.getSellingPrice());
            inventory.setCurrentStock(inventory.getCurrentStock() + request.getInitialStock());
            inventory.setIsClearedByUser(false);
        } else {
            inventory = VendorInventory.builder()
                    .vendor(vendor)
                    .masterProduct(masterProduct)
                    .sellingPrice(request.getSellingPrice())
                    .currentStock(request.getInitialStock())
                    .isClearedByUser(false)
                    .build();
        }

        VendorInventory savedInventory = vendorInventoryRepository.save(inventory);

        if (request.getInitialStock() > 0) {
            StockLedger ledger = StockLedger.builder()
                    .vendorInventory(savedInventory)
                    .entryType(LedgerEntryType.STOCK_ADD)
                    .quantityChange(request.getInitialStock())
                    .balanceAfter(savedInventory.getCurrentStock())
                    .reason("Vendor added catalog product to inventory with initial stock")
                    .build();
            stockLedgerRepository.save(ledger);
        }

        return savedInventory;
    }

    @Transactional
    public MasterProduct toggleProductBan(Long productId, boolean banStatus) {
        MasterProduct product = masterProductRepository.findById(productId)
                .orElseThrow(() -> new ApiException("Product not found", HttpStatus.NOT_FOUND));

        product.setIsBanned(banStatus);
        return masterProductRepository.save(product);
    }

    public Page<MasterProduct> getActiveCatalog(User user, Pageable pageable) {
        if (user != null && user.getRole() != null && user.getRole().name().equals("ROLE_VENDOR")) {
            List<Long> existingIds = vendorInventoryRepository.findActiveMasterProductIdsByVendor(user.getId());
            if (!existingIds.isEmpty()) {
                return masterProductRepository.findAvailableForVendor(existingIds, pageable);
            }
        }
        return masterProductRepository.findByIsApprovedTrueAndIsBannedFalse(pageable);
    }

    public Page<MasterProduct> getApprovalQueue(Pageable pageable) {
        return masterProductRepository.findByIsApprovedFalse(pageable);
    }

    public Page<MasterProduct> getBannedProducts(Pageable pageable) {
        return masterProductRepository.findByIsBannedTrue(pageable);
    }
}