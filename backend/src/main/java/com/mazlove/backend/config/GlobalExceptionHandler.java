package com.mazlove.backend.config;

import com.mazlove.backend.dto.ApiError;
import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ConflictException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.exception.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Transforme toutes les exceptions en réponses JSON au format {@link ApiError}. Spring choisit le
 * gestionnaire le plus spécifique, quel que soit l'ordre des méthodes. Règle : les messages 4xx
 * sont destinés à l'utilisateur (ils disent quoi corriger), le message 500 reste générique pour ne
 * divulguer aucun détail interne.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * @Valid en échec : un message par champ, affichable sous chaque champ du formulaire.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
    ex.getBindingResult()
        .getGlobalErrors()
        .forEach(error -> fields.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
    return build(HttpStatus.BAD_REQUEST, "Certains champs sont à corriger.", fields);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(v -> fields.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
    return build(HttpStatus.BAD_REQUEST, "Certains paramètres sont à corriger.", fields);
  }

  /** JSON mal formé, date illisible, texte à la place d'un nombre… */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
    return build(HttpStatus.BAD_REQUEST, "Le contenu envoyé est illisible ou mal formé.", null);
  }

  /** Ex. GET /api/providers/abc alors qu'un nombre est attendu. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return build(HttpStatus.BAD_REQUEST, "Paramètre « " + ex.getName() + " » invalide.", null);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
    return build(
        HttpStatus.BAD_REQUEST,
        "Paramètre obligatoire manquant : " + ex.getParameterName() + ".",
        null);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
    return build(
        HttpStatus.PAYLOAD_TOO_LARGE, "Le fichier envoyé dépasse la taille autorisée.", null);
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
    return build(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  /**
   * Message technique (souvent en anglais, issu d'une bibliothèque) : on ne le montre pas tel quel.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
    log.debug("Argument invalide : {}", ex.getClass().getSimpleName());
    return build(
        HttpStatus.BAD_REQUEST, "Requête invalide. Vérifiez les informations saisies.", null);
  }

  @ExceptionHandler({UnauthorizedException.class, UsernameNotFoundException.class})
  public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
    String message =
        ex instanceof UnauthorizedException
            ? ex.getMessage()
            : "Votre session a expiré, reconnectez-vous.";
    return build(HttpStatus.UNAUTHORIZED, message, null);
  }

  @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
  public ResponseEntity<ApiError> handleForbidden(RuntimeException ex) {
    String message =
        ex instanceof ForbiddenException
            ? ex.getMessage()
            : "Vous n'avez pas accès à cette ressource.";
    return build(HttpStatus.FORBIDDEN, message, null);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
  }

  @ExceptionHandler({
    ConflictException.class,
    IllegalStateException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /**
   * Course entre deux requêtes (ex. même email inscrit deux fois en même temps) : l'index unique
   * tranche.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
    log.debug("Contrainte d’intégrité violée");
    return build(
        HttpStatus.CONFLICT,
        "Cette action entre en conflit avec des données existantes. Rechargez la page et"
            + " réessayez.",
        null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex) {
    return build(
        HttpStatus.METHOD_NOT_ALLOWED,
        "Méthode " + ex.getMethod() + " non prise en charge sur cette adresse.",
        null);
  }

  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ApiError> handleStatus(
      org.springframework.web.server.ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(
            ApiError.of(
                ex.getStatusCode().value(),
                ex.getReason() == null ? "Action refusée." : ex.getReason()));
  }

  /**
   * Filet de sécurité : ne pas journaliser les données de la requête ni les messages techniques.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
    log.error("Erreur inattendue : {}", ex.getClass().getSimpleName());
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Une erreur inattendue est survenue. Réessayez dans quelques instants.",
        null);
  }

  private static ResponseEntity<ApiError> build(
      HttpStatus status, String message, Map<String, String> fields) {
    return ResponseEntity.status(status).body(ApiError.of(status.value(), message, fields));
  }
}
