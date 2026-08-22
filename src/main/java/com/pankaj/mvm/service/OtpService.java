package com.pankaj.mvm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pankaj.mvm.dto.RegisterRequest;
import com.pankaj.mvm.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    private static final String OTP_PREFIX = "OTP:";
    private static final String REG_DATA_PREFIX = "REG_DATA:";
    private static final String COOLDOWN_PREFIX = "OTP_COOLDOWN:";

    private static final long OTP_TTL_MINUTES = 10;
    private static final long COOLDOWN_MINUTES = 10;

    public void generateAndSendOtp(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String cooldownKey = COOLDOWN_PREFIX + email;

        if (redisTemplate.hasKey(cooldownKey)) {
            long expireSeconds = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            long minutesLeft = (expireSeconds > 0) ? (expireSeconds / 60) + 1 : 10;
            throw new ApiException(
                    "An OTP was already sent. Please wait " + minutesLeft + " minute(s) before requesting another code.",
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(REG_DATA_PREFIX + email, request, OTP_TTL_MINUTES, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(cooldownKey, "LOCKED", COOLDOWN_MINUTES, TimeUnit.MINUTES);

        sendOtpEmail(email, otp);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Multi-Vendor Platform - Registration Verification Code");
            message.setText("Your one-time verification code is: " + otp + "\n\n"
                    + "This code is valid for 10 minutes. Do not share this code with anyone.");
            mailSender.send(message);
            log.info("OTP successfully dispatched to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send OTP email to {}", toEmail, ex);
            redisTemplate.delete(COOLDOWN_PREFIX + toEmail);
            throw new ApiException("Failed to send verification email. Please check your email configuration.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public RegisterRequest validateOtpAndConsume(String email, String inputOtp) {
        String normalizedEmail = email.toLowerCase().trim();
        Object cachedOtpObj = redisTemplate.opsForValue().get(OTP_PREFIX + normalizedEmail);

        if (cachedOtpObj == null || !String.valueOf(cachedOtpObj).equals(inputOtp.trim())) {
            throw new ApiException("Invalid or expired OTP verification code.", HttpStatus.BAD_REQUEST);
        }

        Object regDataObj = redisTemplate.opsForValue().get(REG_DATA_PREFIX + normalizedEmail);
        if (regDataObj == null) {
            throw new ApiException("Registration session expired. Please register again.", HttpStatus.BAD_REQUEST);
        }

        RegisterRequest registrationData;
        try {
            registrationData = objectMapper.convertValue(regDataObj, RegisterRequest.class);
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Failed to parse registration session data.", HttpStatus.BAD_REQUEST);
        }

        redisTemplate.delete(OTP_PREFIX + normalizedEmail);
        redisTemplate.delete(REG_DATA_PREFIX + normalizedEmail);
        redisTemplate.delete(COOLDOWN_PREFIX + normalizedEmail);

        return registrationData;
    }
}