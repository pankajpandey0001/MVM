package com.pankaj.mvm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSellingPriceRequest {

    @NotNull(message = "Master Product ID is required")
    private Long masterProductId;

    @NotNull(message = "New selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    private BigDecimal newSellingPrice;
}