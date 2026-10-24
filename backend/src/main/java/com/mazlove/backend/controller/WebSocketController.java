package com.mazlove.backend.controller;

import com.mazlove.backend.dto.notification.ProviderStatusNotification;
import com.mazlove.backend.service.NotificationService;
import com.mazlove.backend.service.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Messages STOMP envoyés PAR le navigateur (préfixe /app, cf. WebSocketConfig).
 *
 * Les notifications de séance et d'avis ne sont plus acceptées depuis le client : le serveur
 * les émet lui-même au moment de l'action REST (SessionService, ReviewService). Avant, n'importe
 * quel client pouvait diffuser un faux « nouvel avis » ou une notification à tous les abonnés.
 */
@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final NotificationService notificationService;
    private final ProviderService providerService;

    public WebSocketController(NotificationService notificationService, ProviderService providerService) {
        this.notificationService = notificationService;
        this.providerService = providerService;
    }

    /**
     * Un praticien signale qu'il est disponible maintenant (/app/provider-status).
     * Principal = utilisateur authentifié à la connexion STOMP (en-tête Authorization du CONNECT) ;
     * on vérifie qu'il est bien propriétaire de la fiche avant de diffuser.
     */
    @MessageMapping("/provider-status")
    public void handleProviderStatus(Principal principal, @Payload ProviderStatusNotification notification) {
        if (principal == null) {
            log.debug("Statut praticien ignoré : connexion WebSocket anonyme");
            return;
        }
        if (!providerService.isOwner(notification.providerId(), principal.getName())) {
            log.warn("{} a tenté de modifier le statut du praticien {}", principal.getName(), notification.providerId());
            return;
        }
        notificationService.broadcastProviderStatus(notification);
    }
}
