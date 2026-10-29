package com.mazlove.backend.service;

import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.FavoriteRepository;
import com.mazlove.backend.repository.ProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {
    private final FavoriteRepository favorites;
    private final ProviderRepository providers;
    private final ProviderService providerService;
    private final AuthenticatedUserService authenticatedUser;

    public FavoriteService(FavoriteRepository favorites, ProviderRepository providers,
                           ProviderService providerService, AuthenticatedUserService authenticatedUser) {
        this.favorites = favorites;
        this.providers = providers;
        this.providerService = providerService;
        this.authenticatedUser = authenticatedUser;
    }

    @Transactional
    public void add(Long providerId) {
        User user = authenticatedUser.requireUser();
        if (!providers.existsById(providerId)) {
            throw new NotFoundException("Ce praticien n'existe pas ou n'est plus disponible.");
        }
        favorites.addIfAbsent(user.getId(), providerId);
    }

    @Transactional
    public void remove(Long providerId) {
        favorites.remove(authenticatedUser.requireUser().getId(), providerId);
    }

    @Transactional(readOnly = true)
    public List<ProviderDTO> list() {
        Long userId = authenticatedUser.requireUser().getId();
        return providerService.toDTOs(favorites.findByUserId(userId).stream()
                .map(favorite -> favorite.getProvider()).toList(), null, null);
    }
}
