package com.pankaj.mvm.service;

import com.pankaj.mvm.dto.StockAdjustmentRequest;
import com.pankaj.mvm.dto.UpdateSellingPriceRequest;
import com.pankaj.mvm.dto.VendorValuationDashboardResponse;
import com.pankaj.mvm.entity.MasterProduct;
import com.pankaj.mvm.entity.StockLedger;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.entity.VendorInventory;
import com.pankaj.mvm.exceptions.ApiException;
import com.pankaj.mvm.repository.MasterProductRepository;
import com.pankaj.mvm.repository.StockLedgerRepository;
import com.pankaj.mvm.repository.VendorInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorInventoryService {

    private final VendorInventoryRepository vendorInventoryRepository;
    private final MasterProductRepository masterProductRepository;
    private final StockLedgerRepository stockLedgerRepository;

    @Transactional
    public StockLedger adjustStock(User vendor, StockAdjustmentRequest request) {
        MasterProduct masterProduct = masterProductRepository.findById(request.getMasterProductId())
                .orElseThrow(() -> new ApiException("Master Product not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(masterProduct.getIsBanned())) {
            throw new ApiException("Cannot modify stock for a banned product", HttpStatus.BAD_REQUEST);
        }

        VendorInventory inventory = vendorInventoryRepository
                .findByVendorIdAndMasterProductId(vendor.getId(), masterProduct.getId())
                .orElseGet(() -> VendorInventory.builder()
                        .vendor(vendor)
                        .masterProduct(masterProduct)
                        .sellingPrice(masterProduct.getMinPrice())
                        .currentStock(0)
                        .isClearedByUser(false)
                        .build());

        int currentBalance = inventory.getCurrentStock();
        int newBalance;
        int delta = request.getQuantity();

        switch (request.getEntryType()) {
            case STOCK_ADD -> newBalance = currentBalance + delta;
            case STOCK_REMOVE -> {
                if (currentBalance < delta) {
                    throw new ApiException(
                            "Insufficient stock. Available: " + currentBalance + ", Requested removal: " + delta,
                            HttpStatus.BAD_REQUEST
                    );
                }
                newBalance = currentBalance - delta;
            }
            case STOCK_ADJUST_AUDIT -> {
                newBalance = delta;
                delta = newBalance - currentBalance;
            }
            default -> throw new ApiException("Invalid stock movement type", HttpStatus.BAD_REQUEST);
        }

        inventory.setCurrentStock(newBalance);
        VendorInventory savedInventory = vendorInventoryRepository.save(inventory);

        StockLedger ledgerEntry = StockLedger.builder()
                .vendorInventory(savedInventory)
                .entryType(request.getEntryType())
                .quantityChange(delta)
                .balanceAfter(newBalance)
                .reason(request.getReason())
                .build();

        return stockLedgerRepository.save(ledgerEntry);
    }

    @Transactional
    public VendorInventory updateSellingPrice(User vendor, UpdateSellingPriceRequest request) {
        VendorInventory inventory = vendorInventoryRepository
                .findByVendorIdAndMasterProductId(vendor.getId(), request.getMasterProductId())
                .orElseThrow(() -> new ApiException("Inventory record not found for this product", HttpStatus.NOT_FOUND));

        validatePriceCorridor(inventory.getMasterProduct(), request.getNewSellingPrice());

        inventory.setSellingPrice(request.getNewSellingPrice());
        return vendorInventoryRepository.save(inventory);
    }

    private void validatePriceCorridor(MasterProduct product, BigDecimal requestedPrice) {
        BigDecimal min = product.getMinPrice();
        BigDecimal mrp = product.getMrp();

        if (min == null || mrp == null) {
            throw new ApiException("Product price corridor has not been configured by the admin", HttpStatus.BAD_REQUEST);
        }

        if (requestedPrice.compareTo(min) < 0 || requestedPrice.compareTo(mrp) > 0) {
            throw new ApiException(
                    String.format("Selling price ₹%s violates price corridor [₹%s - ₹%s]", requestedPrice, min, mrp),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Transactional
    public void clearBannedProductFromStore(User vendor, Long masterProductId) {
        VendorInventory inventory = vendorInventoryRepository
                .findByVendorIdAndMasterProductId(vendor.getId(), masterProductId)
                .orElseThrow(() -> new ApiException("Inventory record not found", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(inventory.getMasterProduct().getIsBanned())) {
            throw new ApiException("Only banned products can be cleared from active inventory", HttpStatus.BAD_REQUEST);
        }

        inventory.setIsClearedByUser(true);
        vendorInventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public VendorValuationDashboardResponse getValuationDashboard(Long vendorId) {
        BigDecimal valuationMrp = vendorInventoryRepository.calculateValuationAtMrp(vendorId);
        BigDecimal valuationSelling = vendorInventoryRepository.calculateValuationAtSellingPrice(vendorId);

        List<VendorInventory> items = vendorInventoryRepository.findByVendorIdAndIsClearedByUserFalse(vendorId);

        long activeCount = items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getMasterProduct().getIsBanned()))
                .count();

        long bannedCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getMasterProduct().getIsBanned()))
                .count();

        long lowStockCount = items.stream()
                .filter(i -> !Boolean.TRUE.equals(i.getMasterProduct().getIsBanned()) && i.getCurrentStock() <= 10)
                .count();

        return VendorValuationDashboardResponse.builder()
                .totalValuationAtMrp(valuationMrp != null ? valuationMrp : BigDecimal.ZERO)
                .totalValuationAtSellingPrice(valuationSelling != null ? valuationSelling : BigDecimal.ZERO)
                .totalActiveItemsCount(activeCount)
                .totalBannedItemsCount(bannedCount)
                .lowStockItemsCount(lowStockCount)
                .build();
    }

    @Transactional(readOnly = true)
    public VendorValuationDashboardResponse getGlobalPlatformValuation() {
        BigDecimal totalValuationMrp = vendorInventoryRepository.calculateGlobalValuationAtMrp();
        BigDecimal totalValuationSelling = vendorInventoryRepository.calculateGlobalValuationAtSellingPrice();
        BigDecimal mrp = (totalValuationMrp != null) ? totalValuationMrp : BigDecimal.ZERO;
        BigDecimal selling = (totalValuationSelling != null) ? totalValuationSelling : BigDecimal.ZERO;
        BigDecimal potentialProfit = selling.subtract(mrp);

        return VendorValuationDashboardResponse.builder()
                .totalValuationAtMrp(mrp)
                .totalValuationAtSellingPrice(selling)
                .totalPotentialProfit(potentialProfit)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<VendorInventory> getVendorInventory(Long vendorId, Pageable pageable) {
        return vendorInventoryRepository.findByVendorIdAndIsClearedByUserFalse(vendorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockLedger> getVendorLedgerTrail(Long vendorId, Pageable pageable) {
        return stockLedgerRepository.findByVendorInventoryVendorIdOrderByTimestampDesc(vendorId, pageable);
    }
}