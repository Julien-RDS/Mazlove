package com.mazlove.backend.service;

import com.mazlove.backend.dto.ProviderStatsDTO;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.models.Session;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ReviewRepository;
import com.mazlove.backend.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderDashboardServiceTest {
    @Mock AuthenticatedUserService auth;
    @Mock ProviderRepository providers;
    @Mock SessionRepository sessions;
    @Mock ReviewRepository reviews;
    @Mock SessionMapper mapper;
    ProviderDashboardService dashboard;

    @BeforeEach void setup() {
        dashboard = new ProviderDashboardService(auth, providers, sessions, reviews, mapper);
    }

    @Test void rejectsAccountWithoutProviderProfile() {
        User user = new User();
        user.setEmail("client@example.org");
        when(auth.requireUser()).thenReturn(user);
        when(providers.findIdByUserEmail(user.getEmail())).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class, dashboard::stats);
        verifyNoInteractions(sessions, reviews);
    }

    @Test void hidesRevenueWhenHistoricalPricesAreMissing() {
        User user = new User();
        user.setEmail("provider@example.org");
        when(auth.requireUser()).thenReturn(user);
        when(providers.findIdByUserEmail(user.getEmail())).thenReturn(Optional.of(1L));
        when(reviews.summarizeByProviderIds(List.of(1L))).thenReturn(List.of());
        when(sessions.countByProvider_Id(1L)).thenReturn(3L);
        when(sessions.countByProvider_IdAndStatus(1L, Session.Status.DONE)).thenReturn(1L);
        when(sessions.countByProvider_IdAndStatusAndPriceSnapshotIsNull(1L, Session.Status.DONE)).thenReturn(1L);
        ProviderStatsDTO stats = dashboard.stats();
        assertEquals(3, stats.totalSessions());
        assertEquals(1, stats.completedSessions());
        assertNull(stats.revenue());
        verify(sessions, never()).sumRecordedPrices(anyLong(), any());
    }
}
