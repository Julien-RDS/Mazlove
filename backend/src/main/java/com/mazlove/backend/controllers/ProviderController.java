package com.mazlove.backend.controllers;

import com.mazlove.backend.models.Provider;
import com.mazlove.backend.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    @Autowired
    private ProviderRepository providerRepository;

    @GetMapping
    public ResponseEntity<List<Provider>> getProviders(@RequestParam double lat, @RequestParam double lng) {
        // TODO: Implémenter la recherche par géolocalisation
        return ResponseEntity.ok(providerRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Provider> createProvider(@RequestBody Provider provider) {
        return ResponseEntity.ok(providerRepository.save(provider));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Provider> getProvider(@PathVariable Long id) {
        return providerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
