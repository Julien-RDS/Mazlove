package com.mazlove.backend.service;

import com.mazlove.backend.dto.UserUpdateDTO;
import com.mazlove.backend.dto.UserDTO;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public UserDTO getProfile(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDTO)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public UserDTO updateProfile(String email, UserUpdateDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (dto.name() != null) {
            user.setName(dto.name());
        }
        if (dto.phone() != null) {
            user.setPhone(dto.phone());
        }

        return toDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserDTO.SessionDTO> getSessions(String email, int page, int size) {
        return userRepository.findByEmail(email)
                .map(user -> user.getSessions().stream()
                        .map(this::toSessionDTO)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserDTO.ReviewDTO> getReviews(String email, int page, int size) {
        return userRepository.findByEmail(email)
                .map(user -> user.getReviews().stream()
                        .map(this::toReviewDTO)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
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
        return dto;
    }

    private UserDTO.SessionDTO toSessionDTO(com.mazlove.backend.models.Session session) {
        UserDTO.SessionDTO dto = new UserDTO.SessionDTO();
        dto.setId(session.getId());
        dto.setProviderId(session.getProvider().getId());
        dto.setProviderName(session.getProvider().getName());
        dto.setDatetime(session.getDatetime());
        dto.setStatus(session.getStatus());
        dto.setNotes(session.getNotes());
        return dto;
    }

    private UserDTO.ReviewDTO toReviewDTO(com.mazlove.backend.models.Review review) {
        UserDTO.ReviewDTO dto = new UserDTO.ReviewDTO();
        dto.setId(review.getId());
        dto.setSessionId(review.getSessionId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
