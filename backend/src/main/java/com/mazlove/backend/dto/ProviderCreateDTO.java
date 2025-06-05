package com.mazlove.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProviderCreateDTO(
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name is too long")
    String name,
    
    @NotBlank(message = "Description is required")
    String description,
    
    @NotBlank(message = "Category is required")
    String category,
    
    @NotNull(message = "Location is required")
    LocationDTO location,
    
    @NotNull(message = "Availability is required")
    List<TimeSlotDTO> availability,
    
    List<String> images
) {}
