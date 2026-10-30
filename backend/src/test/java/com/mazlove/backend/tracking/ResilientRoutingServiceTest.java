package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class ResilientRoutingServiceTest {
  @Test
  void timeoutAndRedisFailureStillReturnRoute() {
    var osrm = mock(OsrmRoutingService.class);
    when(osrm.route(any(), any(), any())).thenThrow(new RuntimeException("timeout"));
    var redis = mock(StringRedisTemplate.class);
    var r =
        new ResilientRoutingService(
                osrm, new StraightLineRoutingService(), redis, new ObjectMapper())
            .route(new double[] {2.3, 48.8}, new double[] {2.31, 48.81}, TravelMode.WALKING);
    assertTrue(r.estimated());
    assertEquals(2, r.coordinates().size());
    assertTrue(r.durationSeconds() > 0);
  }
}
