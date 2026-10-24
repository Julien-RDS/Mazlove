package com.mazlove.backend.controller;

import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.dto.UserUpdateDTO;
import com.mazlove.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Espace personnel. @AuthenticationPrincipal reçoit le UserDetails posé par
 * JWTAuthenticationFilter : getUsername() renvoie l'email du compte connecté.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Mon compte")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Mon profil (inclut l'id de ma fiche praticien s'il y en a une)")
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @Operation(summary = "Modifier mon nom et/ou mon téléphone")
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateDTO dto
    ) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), dto));
    }

    @Operation(summary = "Mes séances, côté client comme côté praticien")
    @GetMapping("/me/sessions")
    public ResponseEntity<List<UserDTO.SessionDTO>> getSessions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.getSessions(userDetails.getUsername(), page, size));
    }

    @Operation(summary = "Les avis que j'ai laissés")
    @GetMapping("/me/reviews")
    public ResponseEntity<List<UserDTO.ReviewDTO>> getReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(userService.getReviews(userDetails.getUsername(), page, size));
    }
}
