package com.mazlove.backend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final StompAuthChannelInterceptor auth;
  private final List<String> origins;
  private final AuthenticatedSockets sockets;

  public WebSocketConfig(
      StompAuthChannelInterceptor auth,
      AuthenticatedSockets sockets,
      @Value("${cors.allowed-origins}") List<String> origins) {
    this.auth = auth;
    this.sockets = sockets;
    this.origins = origins;
  }

  @Bean
  public ThreadPoolTaskScheduler stompHeartbeatScheduler() {
    var s = new ThreadPoolTaskScheduler();
    s.setPoolSize(1);
    s.setThreadNamePrefix("stomp-heartbeat-");
    return s;
  }

  @Bean(name = "taskScheduler")
  public ThreadPoolTaskScheduler applicationScheduler() {
    var s = new ThreadPoolTaskScheduler();
    s.setPoolSize(4);
    s.setThreadNamePrefix("mazlove-jobs-");
    return s;
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    r.setPreservePublishOrder(true);
    r.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10000, 10000})
        .setTaskScheduler(stompHeartbeatScheduler());
    r.setApplicationDestinationPrefixes("/app");
    r.setUserDestinationPrefix("/user");
  }

  public void registerStompEndpoints(StompEndpointRegistry r) {
    r.addEndpoint("/ws").setAllowedOriginPatterns(origins.toArray(String[]::new));
  }

  public void configureWebSocketTransport(WebSocketTransportRegistration r) {
    r.addDecoratorFactory(
        handler ->
            new org.springframework.web.socket.handler.WebSocketHandlerDecorator(handler) {
              @Override
              public void afterConnectionEstablished(
                  org.springframework.web.socket.WebSocketSession s) throws Exception {
                sockets.connected(s);
                super.afterConnectionEstablished(s);
              }

              @Override
              public void afterConnectionClosed(
                  org.springframework.web.socket.WebSocketSession s,
                  org.springframework.web.socket.CloseStatus status)
                  throws Exception {
                sockets.closed(s.getId());
                super.afterConnectionClosed(s, status);
              }
            });
  }

  public void configureClientOutboundChannel(ChannelRegistration r) {
    r.interceptors(
        new org.springframework.messaging.support.ChannelInterceptor() {
          @Override
          public org.springframework.messaging.Message<?> preSend(
              org.springframework.messaging.Message<?> message,
              org.springframework.messaging.MessageChannel channel) {
            var a = org.springframework.messaging.simp.SimpMessageHeaderAccessor.wrap(message);
            if (a.getMessageType() == org.springframework.messaging.simp.SimpMessageType.MESSAGE
                && !sockets.valid(a.getSessionId())) return null;
            return message;
          }
        });
  }

  public void configureClientInboundChannel(ChannelRegistration r) {
    r.interceptors(auth);
  }
}
