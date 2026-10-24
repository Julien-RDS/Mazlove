package com.mazlove.backend.dto.notification;

/**
 * Événement privé poussé à un participant quand sa séance change d'état
 * (destination STOMP : /user/queue/sessions).
 */
public record SessionNotification(Long sessionId, Long providerId, String status, String message) {}
