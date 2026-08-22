package com.pankaj.mvm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category code is required (e.g., ELEC, APPAR)")
    private String code;

    private Long parentId; // Null for root category
}