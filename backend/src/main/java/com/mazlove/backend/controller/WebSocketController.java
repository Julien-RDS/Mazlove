package com.mazlove.backend.controller;

import com.mazlove.backend.dto.notification.SessionNotification;
import com.mazlove.backend.dto.notification.ReviewNotification;
import com.mazlove.backend.dto.notification.ProviderStatusNotification;
import com.mazlove.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @Autowired
    private NotificationService notificationService;

    @MessageMapping("/session")
    @SendTo("/topic/notifications")
    public SessionNotification handleSessionNotification(@AuthenticationPrincipal UserDetails userDetails, SessionNotification notification) {
        notificationService.sendSessionNotification(userDetails.getUsername(), notification.providerId(), notification.message());
        return notification;
    }

    @MessageMapping("/review")
    @SendTo("/topic/reviews")
    public ReviewNotification handleReviewNotification(@AuthenticationPrincipal UserDetails userDetails, ReviewNotification notification) {
        notificationService.sendReviewNotification(notification.providerId(), notification.message());
        return notification;
    }

    @MessageMapping("/provider-status")
    @SendTo("/topic/providers")
    public ProviderStatusNotification handleProviderStatusNotification(@AuthenticationPrincipal UserDetails userDetails, ProviderStatusNotification notification) {
        notificationService.sendProviderStatusNotification(userDetails.getUsername(), notification.providerId(), notification.available());
        return notification;
    }
}
