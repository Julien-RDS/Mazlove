package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ProviderCreateDTO;
import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.dto.ReviewCreateDTO;
import com.mazlove.backend.dto.ReviewDTO;
import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.service.ProviderService;
import com.mazlove.backend.service.ReviewService;
import com.mazlove.backend.util.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fiches praticiens et leurs avis.
 * Les GET sont publics (on peut parcourir sans compte) ; les POST exigent un JWT (cf. SecurityConfig).
 */
@RestController
@RequestMapping("/api/providers")
@Tag(name = "Praticiens")
public class ProviderController {

    private final ProviderService providerService;
    private final ReviewService reviewService;

    public ProviderController(ProviderService providerService, ReviewService reviewService) {
        this.providerService = providerService;
        this.reviewService = reviewService;
    }

    @Operation(summary = "Praticiens dans un rayon de 50 km, du plus proche au plus éloigné")
    @GetMapping
    public ResponseEntity<List<ProviderDTO>> list(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        checkCoordinates(lat, lng);
        return ResponseEntity.ok(providerService.findNearby(lat, lng, Math.max(page, 0), Paging.clampSize(size)));
    }

    @Operation(summary = "Détail d'un praticien")
    @GetMapping("/{id}")
    public ResponseEntity<ProviderDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getById(id));
    }

    @Operation(summary = "Publier la fiche praticien du compte connecté (rôle PROVIDER)")
    @PostMapping
    public ResponseEntity<ProviderDTO> create(@Valid @RequestBody ProviderCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(dto));
    }

    @Operation(summary = "Praticiens du rayon de 50 km ayant un créneau à une date donnée")
    @GetMapping("/availability")
    public ResponseEntity<List<ProviderDTO>> getAvailable(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String date
    ) {
        checkCoordinates(lat, lng);
        return ResponseEntity.ok(providerService.getAvailableProviders(lat, lng, date));
    }

    @Operation(summary = "Avis publics d'un praticien, du plus récent au plus ancien")
    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDTO>> reviews(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.findByProvider(id));
    }

    @Operation(summary = "Laisser un avis sur une séance terminée avec ce praticien")
    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDTO> addReview(@PathVariable Long id, @Valid @RequestBody ReviewCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(id, dto));
    }

    private static void checkCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new BadRequestException("Coordonnées invalides : latitude entre -90 et 90, longitude entre -180 et 180.");
        }
    }

}
