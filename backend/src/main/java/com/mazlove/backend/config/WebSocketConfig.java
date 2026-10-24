package com.mazlove.backend.config;

import com.mazlove.backend.service.JWTService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Temps réel en STOMP sur WebSocket (repli SockJS pour les réseaux qui bloquent les WebSockets).
 *
 * Côté navigateur : se connecter à /ws en passant "Authorization: Bearer <jwt>" dans les
 * en-têtes de la trame CONNECT, puis s'abonner à /user/queue/sessions (voir NotificationService).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final List<String> allowedOrigins;

    public WebSocketConfig(JWTService jwtService,
                           UserDetailsService userDetailsService,
                           @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:3001}") List<String> allowedOrigins) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic = diffusion publique, /queue = messages privés (via le préfixe /user).
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mêmes origines que l'API REST (auparavant "*", ce qui annulait la restriction CORS).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .withSockJS();
    }

    /**
     * Authentifie la connexion STOMP avec le même JWT que l'API REST. L'utilisateur posé ici
     * devient le Principal des @MessageMapping et la cible de convertAndSendToUser().
     * Sans jeton valide, la connexion reste possible mais anonyme (abonnements publics seulement).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String header = accessor.getFirstNativeHeader("Authorization");
                    if (header != null && header.startsWith("Bearer ")) {
                        jwtService.validateAndGetSubject(header.substring(7).trim()).ifPresent(email -> {
                            try {
                                UserDetails user = userDetailsService.loadUserByUsername(email);
                                accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
                            } catch (UsernameNotFoundException ignored) {
                                // Compte supprimé : la connexion reste anonyme.
                            }
                        });
                    }
                }
                return message;
            }
        });
    }
}
