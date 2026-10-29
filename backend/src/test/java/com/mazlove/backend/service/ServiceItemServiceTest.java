package com.mazlove.backend.service;

import com.mazlove.backend.dto.ServiceItemDTO;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.ServiceItem;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ServiceItemRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceItemServiceTest {
    private final ServiceItemRepository items = mock(ServiceItemRepository.class);
    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final AuthenticatedUserService users = mock(AuthenticatedUserService.class);
    private final ServiceItemService service = new ServiceItemService(items, providers, users);

    @Test
    void ownerCanCreateService() {
        User owner = new User(); owner.setId(1L);
        Provider provider = new Provider(); provider.setId(7L); provider.setUser(owner);
        when(providers.findById(7L)).thenReturn(Optional.of(provider));
        when(users.requireUser()).thenReturn(owner);
        when(items.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ServiceItemDTO result = service.create(7L, new ServiceItemDTO(null, "Séance", "Description", 89f, 2));
        assertEquals(2, result.duration());
        assertEquals(89f, result.price());
        verify(items).save(any(ServiceItem.class));
    }

    @Test
    void anotherUserCannotDeleteAService() {
        User owner = new User(); owner.setId(1L);
        User other = new User(); other.setId(2L);
        Provider provider = new Provider(); provider.setUser(owner);
        when(providers.findById(7L)).thenReturn(Optional.of(provider));
        when(users.requireUser()).thenReturn(other);

        assertThrows(ForbiddenException.class, () -> service.delete(7L, 10L));
        verify(items, never()).delete(any());
    }
}
