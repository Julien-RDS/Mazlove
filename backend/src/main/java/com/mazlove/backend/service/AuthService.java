package com.mazlove.backend.service;

import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void validateCredentials(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
    }

    public String generateJwt(String email) {
        return jwtService.generateToken(email);
    }

    public boolean validateJwt(String token) {
        try {
            String email = jwtService.extractEmail(token);
            return jwtService.isTokenValid(token, email);
        } catch (Exception e) {
            return false;
        }
    }
}
