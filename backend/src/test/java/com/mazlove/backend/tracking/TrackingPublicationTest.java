package com.mazlove.backend.tracking;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TrackingPublicationTest {
  @Test
  void publicationWaitsForCommitAndRollbackNeverPublishes() {
    var action = mock(Runnable.class);
    TransactionSynchronizationManager.initSynchronization();
    try {
      AfterCommit.run(action);
      verifyNoInteractions(action);
      var hook = TransactionSynchronizationManager.getSynchronizations().get(0);
      hook.afterCompletion(1);
      verifyNoInteractions(action);
      hook.afterCommit();
      verify(action).run();
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void stalePublicationAfterArrivalOrDeletionIsSuppressed() {
    var db = mock(JdbcTemplate.class);
    var ws = mock(SimpMessagingTemplate.class);
    var p = new TrackingPublisher(db, ws);
    when(db.queryForList(anyString(), eq(String.class), eq(1L)))
        .thenReturn(List.of("ARRIVED"), List.of(), List.of("EN_ROUTE"));
    p.live(1, "position");
    p.live(1, "position");
    verifyNoInteractions(ws);
    p.live(1, "position");
    verify(ws).convertAndSend("/topic/tracking.1", "position");
  }
}
