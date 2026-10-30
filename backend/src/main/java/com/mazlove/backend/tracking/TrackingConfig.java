package com.mazlove.backend.tracking;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConfigurationProperties(prefix = "tracking")
@Data
public class TrackingConfig {
  private double arrivalRadiusM = 50,
      maxAccuracyM = 100,
      maxSpeedMps = 70,
      deviationThresholdM = 400;
  private int arrivalConsecutivePings = 2,
      deviationDurationS = 60,
      signalLostAfterS = 45,
      etaRefreshS = 30;
}
