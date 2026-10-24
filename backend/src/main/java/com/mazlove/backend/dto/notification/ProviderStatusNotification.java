package com.mazlove.backend.dto.notification;

/** Un praticien signale qu'il est (in)disponible maintenant, diffusé sur /topic/providers. */
public record ProviderStatusNotification(Long providerId, boolean available) {}
