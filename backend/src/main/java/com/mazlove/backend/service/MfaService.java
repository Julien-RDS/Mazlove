package com.mazlove.backend.service;

import com.mazlove.backend.dto.OtpDTO;
import com.mazlove.backend.models.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Date;

@Service
public class MfaService {
    private final Map<String, String> otpCache = new HashMap<>();
    private final int otpExpirationMinutes = 5;
    private final int otpLength = 6;

    public String generateOtp(String email) {
        String otp = generateRandomCode(otpLength);
        otpCache.put(email, otp);
        return otp;
    }

    public boolean verifyOtp(OtpDTO otpDTO) {
        String storedOtp = otpCache.get(otpDTO.email());
        if (storedOtp == null) {
            return false;
        }
        
        boolean isValid = storedOtp.equals(otpDTO.code());
        if (isValid) {
            otpCache.remove(otpDTO.email());
        }
        return isValid;
    }

    private String generateRandomCode(int length) {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, length);
    }

    @Transactional
    public void sendOtp(String email) {
        // Ici, vous pouvez implémenter l'envoi via Twilio/Firebase Auth
        // Pour le moment, on génère simplement le OTP
        String otp = generateOtp(email);
        // TODO: Implémenter l'envoi par email/SMS
        System.out.println("OTP généré pour " + email + ": " + otp);
    }
}
