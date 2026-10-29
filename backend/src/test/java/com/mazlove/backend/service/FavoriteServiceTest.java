package com.mazlove.backend.service;

import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.FavoriteRepository;
import com.mazlove.backend.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FavoriteServiceTest {
    private final FavoriteRepository favorites = mock(FavoriteRepository.class);
    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final ProviderService providerService = mock(ProviderService.class);
    private final AuthenticatedUserService authenticatedUser = mock(AuthenticatedUserService.class);
    private final FavoriteService service = new FavoriteService(favorites, providers, providerService, authenticatedUser);

    @BeforeEach
    void authenticatedUser() {
        User user = new User();
        user.setId(12L);
        when(authenticatedUser.requireUser()).thenReturn(user);
    }

    @Test
    void addUsesAuthenticatedUserAndAtomicIdempotentInsert() {
        when(providers.existsById(4L)).thenReturn(true);
        service.add(4L);
        verify(favorites).addIfAbsent(12L, 4L);
    }

    @Test
    void missingProviderCannotBeAdded() {
        assertThrows(NotFoundException.class, () -> service.add(4L));
        verifyNoInteractions(favorites);
    }

    @Test
    void removeAndListAreScopedToAuthenticatedUser() {
        when(favorites.findByUserId(12L)).thenReturn(List.of());
        service.remove(4L);
        service.list();
        verify(favorites).remove(12L, 4L);
        verify(favorites).findByUserId(12L);
        verify(providerService).toDTOs(List.of(), null, null);
    }
}
