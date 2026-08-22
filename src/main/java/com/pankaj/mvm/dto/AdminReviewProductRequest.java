package com.pankaj.mvm.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class AdminReviewProductRequest {

    @NotNull(message = "Approval status flag is required")
    private Boolean approve;

    private Long categoryId;

    @DecimalMin(value = "0.01", message = "Min price must be greater than zero")
    private BigDecimal minPrice;

    @DecimalMin(value = "0.01", message = "MRP must be greater than zero")
    private BigDecimal mrp;
}