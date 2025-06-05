package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ReviewCreateDTO;
import com.mazlove.backend.dto.ReviewDTO;
import com.mazlove.backend.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return ResponseEntity.status(201).body(reviewService.create(dto));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ReviewDTO>> findByProvider(@PathVariable String providerId) {
        return ResponseEntity.ok(reviewService.findByProvider(providerId));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ReviewDTO> findBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(reviewService.findBySession(sessionId));
    }
}
