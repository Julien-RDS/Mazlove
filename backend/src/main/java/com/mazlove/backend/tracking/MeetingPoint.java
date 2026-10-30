package com.mazlove.backend.tracking;

import jakarta.validation.constraints.*;

public record MeetingPoint(
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double lng,
    @NotBlank @Size(max = 255) String label) {}
