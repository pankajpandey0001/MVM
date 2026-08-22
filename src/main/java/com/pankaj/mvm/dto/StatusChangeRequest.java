package com.pankaj.mvm.dto;

import com.pankaj.mvm.enums.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusChangeRequest {

    @NotNull(message = "Target status is required")
    private AccountStatus targetStatus;

    @NotBlank(message = "Message & Reason is mandatory for status transitions")
    private String reason;
}