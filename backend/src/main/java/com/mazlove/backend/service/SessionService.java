package com.mazlove.backend.service;

import com.mazlove.backend.dto.SessionCreateDTO;
import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.dto.notification.SessionNotification;
import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ConflictException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.ServiceItem;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ServiceItemRepository;
import com.mazlove.backend.repository.SessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Réservation et déroulé d'une séance (voir le cycle de vie dans {@link Session}).
 *
 * <p>Qui fait quoi : - le CLIENT réserve et reçoit le code d'arrivée ; - le PRATICIEN saisit ce
 * code en arrivant, ce qui démarre la séance ; - l'un ou l'autre confirme la fin, ou annule tant
 * que la séance n'a pas commencé.
 */
@Service
public class SessionService {

  /** Statuts qui bloquent le créneau du praticien. */
  private static final Set<Session.Status> ACTIVE_STATUSES =
      EnumSet.of(
          Session.Status.PENDING,
          Session.Status.ACCEPTED,
          Session.Status.EN_ROUTE,
          Session.Status.ARRIVED,
          Session.Status.IN_PROGRESS);

  /** Sans 0/O ni 1/I : le code se dicte et se lit sans ambiguïté, même sur un petit écran. */
  private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

  private static final int CODE_LENGTH = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

  private final SessionRepository sessionRepository;
  private final ProviderRepository providerRepository;
  private final AuthenticatedUserService authenticatedUserService;
  private final SessionMapper sessionMapper;
  private final NotificationService notificationService;
  private final ServiceItemRepository serviceItemRepository;
  private final com.mazlove.backend.tracking.SessionLifecycle lifecycle;

  public SessionService(
      SessionRepository sessionRepository,
      ProviderRepository providerRepository,
      AuthenticatedUserService authenticatedUserService,
      SessionMapper sessionMapper,
      NotificationService notificationService,
      ServiceItemRepository serviceItemRepository,
      com.mazlove.backend.tracking.SessionLifecycle lifecycle) {
    this.lifecycle = lifecycle;
    this.sessionRepository = sessionRepository;
    this.providerRepository = providerRepository;
    this.authenticatedUserService = authenticatedUserService;
    this.sessionMapper = sessionMapper;
    this.notificationService = notificationService;
    this.serviceItemRepository = serviceItemRepository;
  }

  /**
   * Crée une séance entre le client authentifié (lu dans le JWT) et un praticien (cherché via
   * ProviderRepository : c'est bien un Provider, pas un User). Refuse les créneaux hors
   * disponibilités et ceux qui chevauchent une autre séance.
   */
  @Transactional
  public UserDTO.SessionDTO createSession(SessionCreateDTO dto) {
    User client = authenticatedUserService.requireUser();
    Provider provider =
        providerRepository
            .findById(dto.providerId())
            .orElseThrow(
                () -> new NotFoundException("Ce praticien n'existe pas ou n'est plus disponible."));

    if (provider.getUser().getId().equals(client.getId())) {
      throw new BadRequestException(
          "Vous ne pouvez pas réserver une séance avec votre propre fiche.");
    }

    ServiceItem item =
        dto.serviceItemId() == null
            ? null
            : serviceItemRepository
                .findByIdAndProvider_Id(dto.serviceItemId(), provider.getId())
                .orElseThrow(
                    () ->
                        new NotFoundException("Cette prestation n'appartient pas à ce praticien."));
    Integer duration = item == null ? dto.duration() : item.getDuration();
    if (duration == null || duration < 1 || duration > 12) {
      throw new BadRequestException("Choisissez une durée entre 1 et 12 heures.");
    }
    LocalDateTime start = dto.startTime().withSecond(0).withNano(0);
    LocalDateTime end = start.plusHours(duration);
    ensureWithinAvailability(provider, start, end);
    ensureNoOverlap(provider, start, end);

    Session session = new Session();
    if (dto.meetingPoint() == null)
      throw new BadRequestException("Choisissez un point de rendez-vous.");
    if (dto.meetingPoint() != null) {
      session.setMeetingLatitude(dto.meetingPoint().lat());
      session.setMeetingLongitude(dto.meetingPoint().lng());
      session.setMeetingLabel(dto.meetingPoint().label());
    }
    session.setClient(client);
    session.setProvider(provider);
    session.setDatetime(start);
    session.setDuration(duration);
    session.setServiceItem(item);
    session.setPriceSnapshot(item == null ? null : item.getPrice());
    session.setStatus(Session.Status.PENDING);
    session.setCodeValidation(generateValidationCode());
    session.setNotes(dto.notes() == null || dto.notes().isBlank() ? null : dto.notes().trim());

    Session saved = sessionRepository.save(session);
    notificationService.notifySession(
        provider.getUser().getEmail(),
        new SessionNotification(
            saved.getId(),
            provider.getId(),
            saved.getStatus().name(),
            "Nouvelle réservation de " + client.getName()));
    return sessionMapper.toDTO(saved, client);
  }

  /** Détail d'une séance, réservé à ses deux participants. */
  @Transactional(readOnly = true)
  public UserDTO.SessionDTO getSession(Long sessionId) {
    User viewer = authenticatedUserService.requireUser();
    Session session = load(sessionId);
    requireParticipant(session, viewer);
    return sessionMapper.toDTO(session, viewer);
  }

  /**
   * Le praticien saisit le code que le client lui montre : la séance passe à IN_PROGRESS (en
   * cours). Seul le praticien peut le faire, sinon le client pourrait démarrer seul une séance
   * fictive.
   */
  @Transactional
  public UserDTO.SessionDTO validateArrival(Long sessionId, String code) {
    User viewer = authenticatedUserService.requireUser();
    Session session = load(sessionId);

    if (!isProvider(session, viewer)) {
      throw new ForbiddenException(
          isClient(session, viewer)
              ? "C'est votre praticien qui saisit le code d'arrivée : montrez-lui le vôtre."
              : "Cette séance ne vous concerne pas.");
    }
    if (session.getStatus() != Session.Status.ARRIVED) {
      throw new IllegalStateException(
          "Cette séance est déjà " + statusLabel(session.getStatus()) + ".");
    }

    String submitted = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    boolean matches =
        MessageDigest.isEqual(
            session.getCodeValidation().getBytes(StandardCharsets.UTF_8),
            submitted.getBytes(StandardCharsets.UTF_8));
    if (!matches) {
      throw new BadRequestException("Code incorrect. Vérifiez-le avec votre client.");
    }

    lifecycle.move(
        session,
        com.mazlove.backend.tracking.SessionStateMachine.Action.START,
        com.mazlove.backend.tracking.SessionStateMachine.Actor.PROVIDER);
    sessionRepository.save(session);
    notifyOtherParticipant(session, viewer, "Votre praticien est arrivé : la séance commence.");
    return sessionMapper.toDTO(session, viewer);
  }

  /** Fin de séance, par l'un ou l'autre participant, uniquement si elle a commencé. */
  @Transactional
  public UserDTO.SessionDTO confirmEnd(Long sessionId) {
    User viewer = authenticatedUserService.requireUser();
    Session session = load(sessionId);
    requireParticipant(session, viewer);

    if (session.getStatus() != Session.Status.IN_PROGRESS) {
      throw new IllegalStateException(
          session.getStatus() == Session.Status.PENDING
              ? "La séance n'a pas encore commencé : le code d'arrivée doit d'abord être validé."
              : "Cette séance est déjà " + statusLabel(session.getStatus()) + ".");
    }

    lifecycle.move(
        session,
        com.mazlove.backend.tracking.SessionStateMachine.Action.END,
        isProvider(session, viewer)
            ? com.mazlove.backend.tracking.SessionStateMachine.Actor.PROVIDER
            : com.mazlove.backend.tracking.SessionStateMachine.Actor.CLIENT);
    sessionRepository.save(session);
    notifyOtherParticipant(session, viewer, "La séance est terminée.");
    return sessionMapper.toDTO(session, viewer);
  }

  /** Annulation par l'un ou l'autre participant, tant que la séance n'a pas commencé. */
  @Transactional
  public UserDTO.SessionDTO cancel(Long sessionId) {
    User viewer = authenticatedUserService.requireUser();
    Session session = load(sessionId);
    requireParticipant(session, viewer);

    if ((session.getStatus() == Session.Status.PENDING
            || session.getStatus() == Session.Status.ACCEPTED)
        && session.getDatetime().isBefore(LocalDateTime.now().plusHours(24))
        && !lifecycle.hasSos(sessionId)) {
      throw new BadRequestException(
          "L'annulation doit intervenir au moins 24 heures avant la séance.");
    }
    lifecycle.move(
        session,
        com.mazlove.backend.tracking.SessionStateMachine.Action.CANCEL,
        isProvider(session, viewer)
            ? com.mazlove.backend.tracking.SessionStateMachine.Actor.PROVIDER
            : com.mazlove.backend.tracking.SessionStateMachine.Actor.CLIENT);
    sessionRepository.save(session);
    notifyOtherParticipant(
        session, viewer, "La séance a été annulée par " + viewer.getName() + ".");
    return sessionMapper.toDTO(session, viewer);
  }

  // ─── Règles de réservation ───────────────────────────────────────────────

  /**
   * La séance doit tenir entièrement dans un créneau du praticien, le même jour. Les heures sont
   * stockées en "HH:mm" : la comparaison de chaînes suffit.
   */
  private void ensureWithinAvailability(Provider provider, LocalDateTime start, LocalDateTime end) {
    String name = provider.getUser().getName();
    if (!end.toLocalDate().equals(start.toLocalDate())) {
      throw new BadRequestException("La séance doit commencer et se terminer le même jour.");
    }
    String day = start.getDayOfWeek().name();
    String from = start.toLocalTime().format(HOUR_MINUTE);
    String to = end.toLocalTime().format(HOUR_MINUTE);

    boolean fits =
        provider.getAvailability().stream()
            .anyMatch(
                slot ->
                    day.equals(slot.getDayOfWeek())
                        && slot.getStartTime().compareTo(from) <= 0
                        && slot.getEndTime().compareTo(to) >= 0);
    if (!fits) {
      throw new BadRequestException(
          "Ce créneau est en dehors des disponibilités de "
              + name
              + ". Choisissez un horaire affiché sur sa fiche.");
    }
  }

  /**
   * Pas deux séances actives qui se chevauchent chez le même praticien. On remonte de 12 h (durée
   * maximale) pour attraper une séance commencée plus tôt.
   */
  private void ensureNoOverlap(Provider provider, LocalDateTime start, LocalDateTime end) {
    List<Session> nearby =
        sessionRepository.findByProvider_IdAndStatusInAndDatetimeBetween(
            provider.getId(), ACTIVE_STATUSES, start.minusHours(12), end);
    boolean overlaps =
        nearby.stream()
            .anyMatch(
                other -> other.getDatetime().isBefore(end) && other.getEndTime().isAfter(start));
    if (overlaps) {
      throw new ConflictException("Ce créneau est déjà réservé. Choisissez un autre horaire.");
    }
  }

  // ─── Outils ──────────────────────────────────────────────────────────────

  private Session load(Long sessionId) {
    return sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Séance introuvable."));
  }

  private static boolean isClient(Session session, User user) {
    return session.getClient().getId().equals(user.getId());
  }

  private static boolean isProvider(Session session, User user) {
    return session.getProvider().getUser().getId().equals(user.getId());
  }

  private static void requireParticipant(Session session, User user) {
    if (!isClient(session, user) && !isProvider(session, user)) {
      throw new ForbiddenException("Cette séance ne vous concerne pas.");
    }
  }

  /** Prévient en temps réel la personne qui n'a PAS fait l'action. */
  private void notifyOtherParticipant(Session session, User actor, String message) {
    String recipient =
        isClient(session, actor)
            ? session.getProvider().getUser().getEmail()
            : session.getClient().getEmail();
    notificationService.notifySession(
        recipient,
        new SessionNotification(
            session.getId(), session.getProvider().getId(), session.getStatus().name(), message));
  }

  private static String statusLabel(Session.Status status) {
    return switch (status) {
      case PENDING -> "en attente";
      case ACCEPTED -> "acceptée";
      case EN_ROUTE -> "en route";
      case ARRIVED -> "arrivée";
      case IN_PROGRESS -> "en cours";
      case DONE -> "terminée";
      case CANCELLED -> "annulée";
    };
  }

  private static String generateValidationCode() {
    char[] code = new char[CODE_LENGTH];
    for (int i = 0; i < CODE_LENGTH; i++) {
      code[i] = CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)];
    }
    return new String(code);
  }
}
