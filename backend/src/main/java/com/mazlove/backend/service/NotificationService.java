package com.mazlove.backend.service;

import com.mazlove.backend.dto.notification.ProviderStatusNotification;
import com.mazlove.backend.dto.notification.ReviewNotification;
import com.mazlove.backend.dto.notification.SessionNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Envoi des événements temps réel via STOMP (cf. WebSocketConfig).
 *
 * Destinations côté navigateur :
 *  - /user/queue/sessions                  : changements d'état de MES séances (privé)
 *  - /topic/providers/{id}/reviews         : nouveaux avis d'un praticien (public)
 *  - /topic/providers                      : praticiens qui se signalent disponibles (public)
 *
 * Une notification est un « bonus » : son échec ne doit jamais annuler la réservation
 * ou l'avis qui l'a déclenchée, d'où les try/catch qui se contentent de journaliser.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** @param recipientEmail email du destinataire, identique au "name" de sa connexion STOMP */
    public void notifySession(String recipientEmail, SessionNotification notification) {
        try {
            messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/sessions", notification);
        } catch (MessagingException e) {
            log.warn("Notification de séance non envoyée à {} : {}", recipientEmail, e.getMessage());
        }
    }

    public void broadcastReview(ReviewNotification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/providers/" + notification.providerId() + "/reviews", notification);
        } catch (MessagingException e) {
            log.warn("Notification d'avis non diffusée : {}", e.getMessage());
        }
    }

    public void broadcastProviderStatus(ProviderStatusNotification notification) {
        try {
            messagingTemplate.convertAndSend("/topic/providers", notification);
        } catch (MessagingException e) {
            log.warn("Statut praticien non diffusé : {}", e.getMessage());
        }
    }
}
