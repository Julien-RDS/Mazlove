package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {
    private final FavoriteService service;

    public FavoriteController(FavoriteService service) {
        this.service = service;
    }

    @PostMapping("/{providerId}")
    public ResponseEntity<Void> add(@PathVariable Long providerId) {
        service.add(providerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{providerId}")
    public ResponseEntity<Void> remove(@PathVariable Long providerId) {
        service.remove(providerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ProviderDTO> list() {
        return service.list();
    }
}
