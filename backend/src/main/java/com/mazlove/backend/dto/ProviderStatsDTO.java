package com.mazlove.backend.dto;

/** Revenue is null when booked prices have not been recorded. */
public record ProviderStatsDTO(long totalSessions, long completedSessions,
                               Double averageRating, long totalReviews, Double revenue) {}
