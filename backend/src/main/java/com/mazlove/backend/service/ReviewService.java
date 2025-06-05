package com.mazlove.backend.service;

import com.mazlove.backend.dto.ReviewCreateDTO;
import com.mazlove.backend.dto.ReviewDTO;
import com.mazlove.backend.models.Review;
import com.mazlove.backend.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional
    public ReviewDTO create(ReviewCreateDTO dto) {
        Review review = new Review();
        review.setUserId(dto.sessionId());
        review.setSessionId(dto.sessionId());
        review.setRating(dto.rating());
        review.setComment(dto.comment());
        
        Review saved = reviewRepository.save(review);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> findByProvider(String providerId) {
        return reviewRepository.findByProviderId(providerId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReviewDTO findBySession(String sessionId) {
        return reviewRepository.findBySessionId(sessionId)
                .map(this::toDTO)
                .orElse(null);
    }

    private ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setUserId(review.getUserId());
        dto.setSessionId(review.getSessionId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
