package com.mazlove.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;

public record SessionCreateDTO(
    @NotBlank(message = "Provider ID is required")
    String providerId,
    
    @NotNull(message = "Start time is required")
    LocalDateTime startTime,
    
    @Min(value = 1, message = "Duration must be at least 1 hour")
    @Max(value = 12, message = "Duration cannot exceed 12 hours")
    int duration
) {}
