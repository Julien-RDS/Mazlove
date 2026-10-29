package com.mazlove.backend.dto;

import com.mazlove.backend.models.ServiceItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceItemDTO(Long id,
                             @NotBlank @Size(max = 100) String name,
                             @NotBlank @Size(max = 500) String description,
                             @NotNull @DecimalMin("0.0") Float price,
                             @NotNull @Min(1) @Max(12) Integer duration) {
    public static ServiceItemDTO from(ServiceItem item) {
        return new ServiceItemDTO(item.getId(), item.getName(), item.getDescription(), item.getPrice(), item.getDuration());
    }
}
