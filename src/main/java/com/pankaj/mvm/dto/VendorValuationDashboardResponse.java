package com.pankaj.mvm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorValuationDashboardResponse {

    private BigDecimal totalValuationAtMrp;
    private BigDecimal totalValuationAtSellingPrice;
    private BigDecimal totalPotentialProfit;
    private long totalActiveItemsCount;
    private long totalBannedItemsCount;
    private long lowStockItemsCount;
}