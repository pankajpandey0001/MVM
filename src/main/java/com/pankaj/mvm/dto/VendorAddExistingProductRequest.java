package com.pankaj.mvm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class VendorAddExistingProductRequest {

    @NotNull(message = "Master product ID is required")
    private Long masterProductId;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;

    @NotNull(message = "Initial stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer initialStock;
}