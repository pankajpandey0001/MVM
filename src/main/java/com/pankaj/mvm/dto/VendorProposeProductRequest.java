package com.pankaj.mvm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorProposeProductRequest {

    @NotBlank(message = "Product title is required")
    @Size(max = 200, message = "Product title must not exceed 200 characters")
    private String title;

    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}