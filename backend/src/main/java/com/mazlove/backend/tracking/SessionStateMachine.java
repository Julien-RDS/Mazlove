package com.mazlove.backend.tracking;

import com.mazlove.backend.models.Session.Status;
import java.util.*;

/** Pure transition table. Authorization is relative to this session, not the account role. */
public final class SessionStateMachine {
  public enum Actor {
    CLIENT,
    PROVIDER,
    SYSTEM
  }

  public enum Action {
    ACCEPT,
    DECLINE,
    DEPART,
    ARRIVE,
    START,
    END,
    CANCEL
  }

  public record Transition(Action action, Status target, Set<Actor> actors) {}

  private static Transition t(Action a, Status s, Actor... who) {
    return new Transition(a, s, Set.of(who));
  }

  public static final Map<Status, Set<Transition>> TABLE =
      Map.of(
          Status.PENDING,
          Set.of(
              t(Action.ACCEPT, Status.ACCEPTED, Actor.PROVIDER),
              t(Action.DECLINE, Status.CANCELLED, Actor.PROVIDER),
              t(Action.CANCEL, Status.CANCELLED, Actor.CLIENT, Actor.PROVIDER)),
          Status.ACCEPTED,
          Set.of(
              t(Action.DEPART, Status.EN_ROUTE, Actor.PROVIDER),
              t(Action.CANCEL, Status.CANCELLED, Actor.CLIENT, Actor.PROVIDER)),
          Status.EN_ROUTE,
          Set.of(
              t(Action.ARRIVE, Status.ARRIVED, Actor.PROVIDER, Actor.SYSTEM),
              t(Action.CANCEL, Status.CANCELLED, Actor.CLIENT, Actor.PROVIDER)),
          Status.ARRIVED,
          Set.of(
              t(Action.START, Status.IN_PROGRESS, Actor.PROVIDER),
              t(Action.CANCEL, Status.CANCELLED, Actor.CLIENT, Actor.PROVIDER)),
          Status.IN_PROGRESS,
          Set.of(t(Action.END, Status.DONE, Actor.PROVIDER)),
          Status.DONE,
          Set.of(),
          Status.CANCELLED,
          Set.of());

  public static Status next(Status from, Action action, Actor actor) {
    return TABLE.getOrDefault(from, Set.of()).stream()
        .filter(t -> t.action() == action && t.actors().contains(actor))
        .map(Transition::target)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Cette action n'est pas autorisée à cette étape de la séance."));
  }
}
