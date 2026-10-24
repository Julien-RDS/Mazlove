package com.mazlove.backend.service;

import com.mazlove.backend.dto.ReviewCreateDTO;
import com.mazlove.backend.dto.ReviewDTO;
import com.mazlove.backend.dto.notification.ReviewNotification;
import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ConflictException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.Review;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ReviewRepository;
import com.mazlove.backend.repository.SessionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final NotificationService notificationService;
    private final CacheService cacheService;

    public ReviewService(ReviewRepository reviewRepository,
                         SessionRepository sessionRepository,
                         AuthenticatedUserService authenticatedUserService,
                         NotificationService notificationService,
                         CacheService cacheService) {
        this.reviewRepository = reviewRepository;
        this.sessionRepository = sessionRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.notificationService = notificationService;
        this.cacheService = cacheService;
    }

    /** POST /api/reviews : le praticien est déduit de la séance. */
    @Transactional
    public ReviewDTO create(ReviewCreateDTO dto) {
        return create(null, dto);
    }

    /**
     * POST /api/providers/{id}/reviews : même logique, avec contrôle que la séance
     * concerne bien le praticien de l'URL.
     *
     * Conditions pour publier un avis :
     *  1. la séance existe ;
     *  2. l'utilisateur connecté en est le CLIENT (un praticien ne s'auto-évalue pas) ;
     *  3. elle est terminée (DONE) : on ne note pas une séance qui n'a pas eu lieu ;
     *  4. aucun avis n'existe déjà pour elle (doublé par l'index unique SQL).
     */
    @Transactional
    public ReviewDTO create(Long expectedProviderId, ReviewCreateDTO dto) {
        User author = authenticatedUserService.requireUser();
        Session session = sessionRepository.findById(dto.sessionId())
                .orElseThrow(() -> new NotFoundException("Séance introuvable."));

        if (!session.getClient().getId().equals(author.getId())) {
            throw new ForbiddenException("Vous ne pouvez noter que les séances que vous avez réservées.");
        }
        Provider provider = session.getProvider();
        if (expectedProviderId != null && !provider.getId().equals(expectedProviderId)) {
            throw new BadRequestException("Cette séance ne concerne pas ce praticien.");
        }
        if (session.getStatus() != Session.Status.DONE) {
            throw new BadRequestException("Vous pourrez laisser un avis une fois la séance terminée.");
        }
        if (reviewRepository.existsBySession_Id(session.getId())) {
            throw new ConflictException("Vous avez déjà laissé un avis pour cette séance.");
        }

        Review review = new Review();
        review.setUser(author);
        review.setSession(session);
        review.setProvider(provider);
        review.setRating(dto.rating());
        review.setComment(dto.comment() == null || dto.comment().isBlank() ? null : dto.comment().trim());

        Review saved = reviewRepository.save(review);

        // La note moyenne change : les avis ET les listes de praticiens sont périmés.
        cacheService.evictProviderReviews(provider.getId());
        cacheService.evictNearbyProviders();
        notificationService.broadcastReview(
                new ReviewNotification(provider.getId(), saved.getRating(), "Nouvel avis publié"));

        return toDTO(saved);
    }

    @Cacheable(cacheNames = CacheService.REVIEWS, key = "#providerId")
    @Transactional(readOnly = true)
    public List<ReviewDTO> findByProvider(Long providerId) {
        return reviewRepository.findByProviderId(providerId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public ReviewDTO findBySession(Long sessionId) {
        return reviewRepository.findBySessionId(sessionId)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("Aucun avis pour cette séance."));
    }

    private ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setUserId(review.getUser().getId());
        dto.setAuthorName(shortName(review.getUser().getName()));
        dto.setSessionId(review.getSession().getId());
        dto.setProviderId(review.getProvider().getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    /** "Camille Dupont" -> "Camille D." : assez pour humaniser l'avis, pas assez pour identifier. */
    static String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Membre Mazlove";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + Character.toUpperCase(parts[parts.length - 1].charAt(0)) + ".";
    }
}
