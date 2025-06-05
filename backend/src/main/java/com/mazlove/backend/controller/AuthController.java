package com.mazlove.backend.controller;

import com.mazlove.backend.dto.LoginDTO;
import com.mazlove.backend.dto.OtpDTO;
import com.mazlove.backend.dto.TokenDTO;
import com.mazlove.backend.service.AuthService;
import com.mazlove.backend.service.MfaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final MfaService mfaService;

    public AuthController(AuthService authService, MfaService mfaService) {
        this.authService = authService;
        this.mfaService = mfaService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(@Valid @RequestBody LoginDTO loginDTO) {
        // Valider les identifiants
        authService.validateCredentials(loginDTO.email(), loginDTO.password());
        
        // Générer et envoyer l'OTP
        mfaService.sendOtp(loginDTO.email());
        return ResponseEntity.ok().body(new TokenDTO("otp:" + mfaService.generateOtp(loginDTO.email())));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<TokenDTO> verifyOtp(@Valid @RequestBody OtpDTO otpDTO) {
        if (!mfaService.verifyOtp(otpDTO)) {
            return ResponseEntity.badRequest().body(new TokenDTO("error:OTP invalid"));
        }
        
        // Générer le JWT final
        String jwt = authService.generateJwt(otpDTO.email());
        return ResponseEntity.ok().body(new TokenDTO(jwt));
    }
}
