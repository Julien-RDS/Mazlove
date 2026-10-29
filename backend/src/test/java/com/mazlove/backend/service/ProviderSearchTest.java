package com.mazlove.backend.service;

import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProviderSearchTest {
    private final ProviderRepository providers = mock(ProviderRepository.class);
    private final ProviderService service = new ProviderService(providers, mock(ReviewRepository.class),
            mock(AuthenticatedUserService.class), mock(CacheService.class));

    @Test
    void filtersAndPaginationReachDatabaseBeforeMapping() {
        when(providers.searchNearby(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), anyBoolean(), anyString(), anyBoolean(), anyDouble(),
                anyBoolean(), anyString(), anyInt(), anyLong()))
                .thenReturn(List.of());
        service.searchNearby(48.87, 2.35, 2, 10, " Relaxation ", 4.0, "2026-10-26");
        verify(providers).searchNearby(eq(48.87), eq(2.35), eq(50.0), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), eq(true), eq("Relaxation"), eq(true), eq(4.0),
                eq(true), eq("MONDAY"), eq(10), eq(20L));
    }

    @Test
    void absentFiltersUseTypedValuesAndLargeOffsetsDoNotOverflow() {
        service.searchNearby(48, 2, Integer.MAX_VALUE, 10, null, null, null);
        verify(providers).searchNearby(eq(48.0), eq(2.0), eq(50.0), anyDouble(), anyDouble(),
                anyDouble(), anyDouble(), eq(false), eq(""), eq(false), eq(0.0),
                eq(false), eq(""), eq(10), eq(21474836470L));
    }

    @Test
    void invalidRatingAndDayAreRejected() {
        assertThrows(BadRequestException.class, () -> service.searchNearby(48, 2, 0, 10, null, 5.1, null));
        assertThrows(BadRequestException.class, () -> service.searchNearby(48, 2, 0, 10, null, Double.NaN, null));
        assertThrows(BadRequestException.class, () -> service.searchNearby(48, 2, 0, 10, null, null, "mardi"));
        verifyNoInteractions(providers);
    }
}
