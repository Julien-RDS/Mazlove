package com.mazlove.backend.service;

import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ReviewRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conversion Session -> UserDTO.SessionDTO, partagée par SessionService et UserService.
 *
 * La vue dépend de QUI regarde : le code d'arrivée n'est montré qu'au client
 * (s'il était visible du praticien, celui-ci pourrait « valider » sans être venu).
 * À appeler dans une transaction : on lit des relations LAZY (noms client/praticien).
 */
@Component
public class SessionMapper {

    private final ReviewRepository reviewRepository;

    public SessionMapper(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public UserDTO.SessionDTO toDTO(Session session, User viewer) {
        return toDTO(session, viewer, reviewRepository.existsBySession_Id(session.getId()));
    }

    /** Version liste : une seule requête pour savoir quelles séances ont déjà un avis. */
    public List<UserDTO.SessionDTO> toDTOs(List<Session> sessions, User viewer) {
        if (sessions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = sessions.stream().map(Session::getId).collect(Collectors.toList());
        Set<Long> reviewed = new HashSet<>(reviewRepository.findReviewedSessionIds(ids));
        return sessions.stream()
                .map(s -> toDTO(s, viewer, reviewed.contains(s.getId())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private UserDTO.SessionDTO toDTO(Session session, User viewer, boolean reviewed) {
        boolean viewerIsClient = session.getClient().getId().equals(viewer.getId());

        UserDTO.SessionDTO dto = new UserDTO.SessionDTO();
        dto.setId(session.getId());
        dto.setProviderId(session.getProvider().getId());
        dto.setProviderName(session.getProvider().getUser().getName());
        dto.setClientName(session.getClient().getName());
        dto.setDatetime(session.getDatetime());
        dto.setDuration(session.getDuration());
        dto.setServiceItemId(session.getServiceItem() == null ? null : session.getServiceItem().getId());
        dto.setPrice(session.getPriceSnapshot());
        dto.setStatus(session.getStatus());
        dto.setNotes(session.getNotes());
        dto.setViewerRole(viewerIsClient ? "CLIENT" : "PROVIDER");
        dto.setArrivalCode(viewerIsClient ? session.getCodeValidation() : null);
        dto.setReviewed(reviewed);
        return dto;
    }
}
