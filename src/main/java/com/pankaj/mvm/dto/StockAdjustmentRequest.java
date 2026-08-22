package com.pankaj.mvm.dto;

import com.pankaj.mvm.enums.LedgerEntryType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {

    @NotNull(message = "Master Product ID is required")
    private Long masterProductId;

    @NotNull(message = "Entry type must be STOCK_ADD, STOCK_REMOVE, or STOCK_ADJUST_AUDIT")
    private LedgerEntryType entryType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity change must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Audit reason is required (e.g., 'Supplier Shipment', 'Damaged Stock')")
    private String reason;
}