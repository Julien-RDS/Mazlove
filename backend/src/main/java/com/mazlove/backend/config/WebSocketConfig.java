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

  public WebSocketConfig(
      StompAuthChannelInterceptor auth, @Value("${cors.allowed-origins}") List<String> origins) {
    this.auth = auth;
    this.origins = origins;
  }

  @Bean
  public ThreadPoolTaskScheduler stompHeartbeatScheduler() {
    var s = new ThreadPoolTaskScheduler();
    s.setPoolSize(1);
    s.setThreadNamePrefix("stomp-heartbeat-");
    return s;
  }

  public void configureMessageBroker(MessageBrokerRegistry r) {
    r.enableSimpleBroker("/topic", "/queue")
        .setHeartbeatValue(new long[] {10000, 10000})
        .setTaskScheduler(stompHeartbeatScheduler());
    r.setApplicationDestinationPrefixes("/app");
    r.setUserDestinationPrefix("/user");
  }

  public void registerStompEndpoints(StompEndpointRegistry r) {
    r.addEndpoint("/ws").setAllowedOriginPatterns(origins.toArray(String[]::new));
  }

  public void configureClientInboundChannel(ChannelRegistration r) {
    r.interceptors(auth);
  }
}
