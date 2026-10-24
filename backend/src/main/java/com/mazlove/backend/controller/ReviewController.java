package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ReviewCreateDTO;
import com.mazlove.backend.dto.ReviewDTO;
import com.mazlove.backend.service.ReviewService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Accès aux avis par séance ou par praticien.
 * Équivalent « à plat » de /api/providers/{id}/reviews, conservé pour les clients existants.
 */
@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Avis")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Laisser un avis (le praticien est déduit de la séance)")
    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(dto));
    }

    @Operation(summary = "Avis d'un praticien")
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReviewDTO>> findByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(reviewService.findByProvider(providerId));
    }

    @Operation(summary = "Avis laissé sur une séance (404 s'il n'y en a pas)")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ReviewDTO> findBySession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(reviewService.findBySession(sessionId));
    }
}
