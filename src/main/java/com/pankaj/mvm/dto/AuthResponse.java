package com.pankaj.mvm.dto;

import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
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
public class AuthResponse {
    private String token;
    private String email;
    private String fullName;
    private Role role;
    private AccountStatus status;
    private String message;
}