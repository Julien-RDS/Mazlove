package com.mazlove.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendSessionNotification(String userId, String providerId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/topic/notifications",
                new SessionNotification(providerId, message)
        );
    }

    public void sendReviewNotification(String providerId, String message) {
        messagingTemplate.convertAndSend(
                "/topic/providers/" + providerId + "/notifications",
                new ReviewNotification(message)
        );
    }

    public void sendProviderStatusNotification(String userId, String providerId, boolean available) {
        messagingTemplate.convertAndSendToUser(
                userId,
                "/topic/notifications",
                new ProviderStatusNotification(providerId, available)
        );
    }
}
