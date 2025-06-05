package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ProviderCreateDTO;
import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {
    private final ProviderService providerService;

    @Autowired
    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderDTO>> list(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(providerService.findNearby(lat, lng, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderDTO> get(@PathVariable String id) {
        return ResponseEntity.ok(providerService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProviderDTO> create(@Valid @RequestBody ProviderCreateDTO dto) {
        return ResponseEntity.status(201).body(providerService.create(dto));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<ProviderDTO>> getAvailable(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String date
    ) {
        return ResponseEntity.ok(providerService.getAvailableProviders(lat, lng, date));
    }
}
