package com.mazlove.backend.service;

import com.mazlove.backend.dto.ProviderStatsDTO;
import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ReviewRepository;
import com.mazlove.backend.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProviderDashboardService {
  private final AuthenticatedUserService auth;
  private final ProviderRepository providers;
  private final SessionRepository sessions;
  private final ReviewRepository reviews;
  private final SessionMapper mapper;

  public ProviderDashboardService(
      AuthenticatedUserService auth,
      ProviderRepository providers,
      SessionRepository sessions,
      ReviewRepository reviews,
      SessionMapper mapper) {
    this.auth = auth;
    this.providers = providers;
    this.sessions = sessions;
    this.reviews = reviews;
    this.mapper = mapper;
  }

  private Long providerId(User user) {
    return providers
        .findIdByUserEmail(user.getEmail())
        .orElseThrow(() -> new ForbiddenException("Vous devez posséder une fiche praticien."));
  }

  public List<UserDTO.SessionDTO> upcoming() {
    User user = auth.requireUser();
    return mapper.toDTOs(
        sessions.findUpcoming(
            providerId(user),
            List.of(
                Session.Status.PENDING,
                Session.Status.ACCEPTED,
                Session.Status.EN_ROUTE,
                Session.Status.ARRIVED,
                Session.Status.IN_PROGRESS),
            LocalDateTime.now()),
        user);
  }

  public List<UserDTO.SessionDTO> history() {
    User user = auth.requireUser();
    return mapper.toDTOs(
        sessions.findHistory(
            providerId(user), List.of(Session.Status.DONE, Session.Status.CANCELLED)),
        user);
  }

  public ProviderStatsDTO stats() {
    Long id = providerId(auth.requireUser());
    List<ReviewRepository.RatingSummary> summaries = reviews.summarizeByProviderIds(List.of(id));
    Double average = summaries.isEmpty() ? null : summaries.get(0).getAverage();
    long totalReviews = summaries.isEmpty() ? 0 : summaries.get(0).getTotal();
    long completed = sessions.countByProvider_IdAndStatus(id, Session.Status.DONE);
    Double revenue = null;
    if (sessions.countByProvider_IdAndStatusAndPriceSnapshotIsNull(id, Session.Status.DONE) == 0) {
      Double sum = sessions.sumRecordedPrices(id, Session.Status.DONE);
      revenue = sum == null ? 0.0 : sum;
    }
    return new ProviderStatsDTO(
        sessions.countByProvider_Id(id), completed, average, totalReviews, revenue);
  }
}
