package com.mazlove.backend.service;

import com.mazlove.backend.dto.SessionCreateDTO;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.ServiceItem;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ServiceItemRepository;
import com.mazlove.backend.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionServiceItemTest {
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final AuthenticatedUserService users = mock(AuthenticatedUserService.class);
    private final SessionMapper mapper = mock(SessionMapper.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final ServiceItemRepository items = mock(ServiceItemRepository.class);
    private final SessionService service = new SessionService(sessions, providers, users, mapper, notifications, items);

    @Test
    void bookingUsesServiceDurationAndSnapshotsPrice() {
        User client = new User(); client.setId(1L); client.setName("Client");
        User owner = new User(); owner.setId(2L); owner.setName("Provider"); owner.setEmail("p@example.com");
        LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
        Provider provider = new Provider(); provider.setId(3L); provider.setUser(owner);
        provider.setAvailability(Set.of(new Provider.TimeSlot(start.getDayOfWeek().name(), "09:00", "18:00")));
        ServiceItem item = new ServiceItem(); item.setId(4L); item.setProvider(provider); item.setDuration(2); item.setPrice(89f);
        when(users.requireUser()).thenReturn(client);
        when(providers.findById(3L)).thenReturn(Optional.of(provider));
        when(items.findByIdAndProvider_Id(4L, 3L)).thenReturn(Optional.of(item));
        when(sessions.findByProvider_IdAndStatusInAndDatetimeBetween(eq(3L), any(), any(), any())).thenReturn(List.of());
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createSession(new SessionCreateDTO(3L, start, 1, null, 4L));

        ArgumentCaptor<Session> saved = ArgumentCaptor.forClass(Session.class);
        verify(sessions).save(saved.capture());
        assertEquals(2, saved.getValue().getDuration());
        assertEquals(89f, saved.getValue().getPriceSnapshot());
        assertSame(item, saved.getValue().getServiceItem());
    }

    @Test
    void rejectsServiceFromAnotherProvider() {
        User client = new User(); client.setId(1L);
        User owner = new User(); owner.setId(2L);
        Provider provider = new Provider(); provider.setId(3L); provider.setUser(owner);
        when(users.requireUser()).thenReturn(client);
        when(providers.findById(3L)).thenReturn(Optional.of(provider));
        when(items.findByIdAndProvider_Id(4L, 3L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.createSession(
                new SessionCreateDTO(3L, LocalDateTime.now().plusDays(1), null, null, 4L)));
        verify(sessions, never()).save(any());
    }
}
