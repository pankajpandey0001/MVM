package com.pankaj.mvm.service;

import com.pankaj.mvm.dto.AuthResponse;
import com.pankaj.mvm.dto.LoginRequest;
import com.pankaj.mvm.dto.RegisterRequest;
import com.pankaj.mvm.dto.VerifyOtpRequest;
import com.pankaj.mvm.entity.User;
import com.pankaj.mvm.enums.AccountStatus;
import com.pankaj.mvm.enums.Role;
import com.pankaj.mvm.exceptions.ApiException;
import com.pankaj.mvm.repository.UserRepository;
import com.pankaj.mvm.security.JwtUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final OtpService otpService;

    // Injected properties from application.properties
    @Value("${app.admin.full-name:Super Admin}")
    private String adminFullName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostConstruct
    public void seedSuperAdmin() {
        if (!userRepository.existsByEmail(adminEmail)) {
            User superAdmin = User.builder()
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .phone(adminPhone)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.SUPER_ADMIN)
                    .status(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(superAdmin);
            log.info("Super Admin account seeded.......Done........");
        }
    }

    public String initiateRegistration(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String phone = request.getPhone().trim();

        if (userRepository.existsByEmail(email)) {
            throw new ApiException("An account with this email already exists.", HttpStatus.CONFLICT);
        }

        if (userRepository.existsByPhone(phone)) {
            throw new ApiException("An account with this phone number already exists.", HttpStatus.CONFLICT);
        }

        otpService.generateAndSendOtp(request);
        return "Verification OTP has been dispatched to " + email + ". Valid for 10 minutes.";
    }

    public String verifyOtpAndRegister(VerifyOtpRequest request) {
        RegisterRequest regData = otpService.validateOtpAndConsume(request.getEmail(), request.getOtp());

        User user = User.builder()
                .fullName(regData.getFullName().trim())
                .email(regData.getEmail().toLowerCase().trim())
                .phone(regData.getPhone().trim())
                .password(passwordEncoder.encode(regData.getPassword()))
                .role(regData.getRole())
                .status(AccountStatus.PENDING)
                .build();

        userRepository.save(user);
        return "Registration successful. Your account is currently PENDING review by the Super Admin.";
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid credentials.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials.", HttpStatus.UNAUTHORIZED);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ApiException("Account is not active. Current status: " + user.getStatus(), HttpStatus.FORBIDDEN);
        }

        String token = jwtUtils.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}