package com.mazlove.backend.controller;

import com.mazlove.backend.dto.ServiceItemDTO;
import com.mazlove.backend.service.ServiceItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/providers/{providerId}/services")
public class ServiceItemController {
    private final ServiceItemService service;

    public ServiceItemController(ServiceItemService service) { this.service = service; }

    @GetMapping
    public List<ServiceItemDTO> list(@PathVariable Long providerId) { return service.list(providerId); }

    @PostMapping
    public ResponseEntity<ServiceItemDTO> create(@PathVariable Long providerId, @Valid @RequestBody ServiceItemDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(providerId, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long providerId, @PathVariable Long id) {
        service.delete(providerId, id);
        return ResponseEntity.noContent().build();
    }
}
