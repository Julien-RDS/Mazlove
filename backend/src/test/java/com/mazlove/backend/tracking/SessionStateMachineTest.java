package com.mazlove.backend.tracking;

import static com.mazlove.backend.tracking.SessionStateMachine.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mazlove.backend.models.Session.Status;
import org.junit.jupiter.api.Test;

class SessionStateMachineTest {
  @Test
  void exhaustiveRoleAndStateMatrix() {
    for (Status s : Status.values())
      for (Action a : Action.values())
        for (Actor actor : Actor.values()) {
          var transition =
              TABLE.get(s).stream()
                  .filter(t -> t.action() == a && t.actors().contains(actor))
                  .findFirst();
          if (transition.isPresent()) assertEquals(transition.get().target(), next(s, a, actor));
          else assertThrows(IllegalStateException.class, () -> next(s, a, actor));
        }
  }

  @Test
  void codeIsRequiredAfterArrival() {
    assertEquals(Status.IN_PROGRESS, next(Status.ARRIVED, Action.START, Actor.PROVIDER));
    assertThrows(
        IllegalStateException.class, () -> next(Status.ACCEPTED, Action.START, Actor.PROVIDER));
    assertThrows(
        IllegalStateException.class, () -> next(Status.IN_PROGRESS, Action.END, Actor.CLIENT));
  }
}
