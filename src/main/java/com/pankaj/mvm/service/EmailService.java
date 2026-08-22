package com.pankaj.mvm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Registration OTP - Enterprise Multi-Vendor Platform");
        message.setText("Welcome to the Enterprise Platform!\n\n" +
                "Your one-time verification code is: " + otp + "\n\n" +
                "This code will expire in 5 minutes.\n" +
                "If you did not request this verification, please ignore this email.");
        mailSender.send(message);
    }
}