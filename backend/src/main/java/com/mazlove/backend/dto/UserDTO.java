package com.mazlove.backend.dto;

import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Profil de l'utilisateur connecté (GET /api/users/me) et vues associées. Le mot de passe
 * n'apparaît évidemment jamais ici.
 */
@Data
public class UserDTO {
  private Long id;
  private String email;
  private String name;
  private String phone;
  private User.Role role;
  private float rating;
  private String avatar;

  /** Id de la fiche praticien du compte, ou null s'il n'en a pas encore créé. */
  private Long providerId;

  /**
   * Séance vue par l'un de ses participants. Sert pour la liste « Mes séances » et pour toutes les
   * réponses de /api/sessions.
   */
  @Data
  public static class SessionDTO {
    private Long id;
    private Long providerId;
    private String providerName;
    private String clientName;
    private LocalDateTime datetime;
    private int duration;
    private Long serviceItemId;
    private Float price;
    private Session.Status status;
    private String notes;
    private com.mazlove.backend.tracking.MeetingPoint meetingPoint;
    private com.mazlove.backend.tracking.TravelMode travelMode;

    /** Code d'arrivée : rempli uniquement quand c'est le client qui consulte. */
    private String arrivalCode;

    /** Rôle de la personne qui consulte dans CETTE séance : "CLIENT" ou "PROVIDER". */
    private String viewerRole;

    /** true si le client a déjà laissé son avis (le bouton « Laisser un avis » disparaît). */
    private boolean reviewed;
  }

  /** Avis rédigé par l'utilisateur connecté (pas besoin de répéter son identité). */
  @Data
  public static class ReviewDTO {
    private Long id;
    private Long sessionId;
    private Long providerId;
    private String providerName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
  }
}
