package com.mazlove.backend.service;

import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.dto.UserUpdateDTO;
import com.mazlove.backend.models.Review;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.UserRepository;
import com.mazlove.backend.util.Paging;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profil de l'utilisateur connecté. Sert aussi de UserDetailsService à Spring Security :
 * JWTAuthenticationFilter l'appelle à chaque requête pour recharger le compte à partir de l'email
 * contenu dans le JWT.
 */
@Service
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private final ProviderRepository providerRepository;
  private final SessionMapper sessionMapper;

  public UserService(
      UserRepository userRepository,
      ProviderRepository providerRepository,
      SessionMapper sessionMapper) {
    this.userRepository = userRepository;
    this.providerRepository = providerRepository;
    this.sessionMapper = sessionMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));
    // Un compte ancien sans rôle est traité comme client plutôt que de faire planter
    // l'authentification.
    User.Role role = user.getRole() != null ? user.getRole() : User.Role.CLIENT;
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getEmail())
        .password(user.getPassword())
        .roles(role.name())
        .build();
  }

  @Transactional(readOnly = true)
  public UserDTO getProfile(String email) {
    return userRepository
        .findByEmail(email)
        .map(this::toDTO)
        .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));
  }

  @Transactional(readOnly = true)
  public UserDTO.SessionDTO getActiveSession(String email, boolean providerOnly) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));
    return userRepository
        .findActiveSession(user.getId(), providerOnly, java.time.LocalDateTime.now())
        .map(s -> sessionMapper.toDTO(s, user))
        .orElse(null);
  }

  /** Mise à jour partielle : les champs null ou vides sont ignorés. */
  @Transactional
  public UserDTO updateProfile(String email, UserUpdateDTO dto) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));

    if (dto.name() != null && !dto.name().isBlank()) {
      user.setName(dto.name().trim());
    }
    if (dto.phone() != null && !dto.phone().isBlank()) {
      user.setPhone(dto.phone().trim());
    }
    return toDTO(userRepository.save(user));
  }

  /** Séances où l'utilisateur est client ou praticien, paginées (taille bornée à 50). */
  @Transactional(readOnly = true)
  public List<UserDTO.SessionDTO> getSessions(String email, int page, int size) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));
    return sessionMapper.toDTOs(
        userRepository.findSessionsByUserId(user.getId(), Paging.of(page, size)), user);
  }

  /** Avis rédigés par l'utilisateur, paginés (taille bornée à 50). */
  @Transactional(readOnly = true)
  public List<UserDTO.ReviewDTO> getReviews(String email, int page, int size) {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable"));
    return userRepository.findReviewsByUserId(user.getId(), Paging.of(page, size)).stream()
        .map(this::toReviewDTO)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private UserDTO toDTO(User user) {
    UserDTO dto = new UserDTO();
    dto.setId(user.getId());
    dto.setEmail(user.getEmail());
    dto.setName(user.getName());
    dto.setPhone(user.getPhone());
    dto.setRole(user.getRole());
    dto.setRating(user.getRating());
    dto.setAvatar(user.getAvatar());
    dto.setProviderId(providerRepository.findIdByUserEmail(user.getEmail()).orElse(null));
    return dto;
  }

  /** Pas d'id utilisateur ici : c'est forcément l'utilisateur connecté. */
  private UserDTO.ReviewDTO toReviewDTO(Review review) {
    UserDTO.ReviewDTO dto = new UserDTO.ReviewDTO();
    dto.setId(review.getId());
    dto.setSessionId(review.getSession().getId());
    dto.setProviderId(review.getProvider().getId());
    dto.setProviderName(review.getProvider().getUser().getName());
    dto.setRating(review.getRating());
    dto.setComment(review.getComment());
    dto.setCreatedAt(review.getCreatedAt());
    return dto;
  }
}
