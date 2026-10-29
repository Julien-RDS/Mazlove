package com.mazlove.backend.service;

import com.mazlove.backend.dto.LocationDTO;
import com.mazlove.backend.dto.ProviderCreateDTO;
import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.dto.TimeSlotDTO;
import com.mazlove.backend.exception.BadRequestException;
import com.mazlove.backend.exception.ConflictException;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.User;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ReviewRepository;
import com.mazlove.backend.util.GeoUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProviderService {

    /** "9:00" ou "09:00" acceptés en entrée… */
    private static final DateTimeFormatter INPUT_TIME = DateTimeFormatter.ofPattern("H:mm");
    /** …toujours stockés sur 5 caractères pour que la comparaison de chaînes reste chronologique. */
    private static final DateTimeFormatter STORED_TIME = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Rayon de recherche d'origine (50 km), conservé tel quel : les critères de sélection des
     * praticiens seront revus ultérieurement par l'équipe produit, ne pas le changer ici.
     */
    public static final double SEARCH_RADIUS_KM = 50;

    private final ProviderRepository providerRepository;
    private final ReviewRepository reviewRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CacheService cacheService;

    public ProviderService(ProviderRepository providerRepository,
                           ReviewRepository reviewRepository,
                           AuthenticatedUserService authenticatedUserService,
                           CacheService cacheService) {
        this.providerRepository = providerRepository;
        this.reviewRepository = reviewRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.cacheService = cacheService;
    }

    /**
     * Praticiens à moins de 50 km d'un point, du plus proche au plus éloigné (critère d'origine).
     * Résultat mis en cache Redis, clé = coordonnées exactes + pagination.
     */
    @Cacheable(cacheNames = CacheService.PROVIDERS, key = "#lat + ':' + #lng + ':' + #page + ':' + #size")
    @Transactional(readOnly = true)
    public List<ProviderDTO> findNearby(double lat, double lng, int page, int size) {
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(lat, lng, SEARCH_RADIUS_KM);
        List<Provider> providers = providerRepository.findNearby(
                lat, lng, SEARCH_RADIUS_KM,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng(),
                size, page * size);
        return toDTOs(providers, lat, lng);
    }

    /** Filtres appliqués avant la pagination ; la note vient des avis, sans colonne dénormalisée. */
    @Transactional(readOnly = true)
    public List<ProviderDTO> searchNearby(double lat, double lng, int page, int size,
                                          String category, Double minRating, String date) {
        if (minRating != null && (!Double.isFinite(minRating) || minRating < 0 || minRating > 5)) {
            throw new BadRequestException("La note minimale doit être comprise entre 0 et 5.");
        }
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        String day = date == null || date.isBlank() ? null : resolveDayOfWeek(date);
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(lat, lng, SEARCH_RADIUS_KM);
        List<Provider> providers = providerRepository.searchNearby(lat, lng, SEARCH_RADIUS_KM,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng(),
                normalizedCategory != null, normalizedCategory == null ? "" : normalizedCategory,
                minRating != null, minRating == null ? 0.0 : minRating,
                day != null, day == null ? "" : day, size, (long) page * size);
        return toDTOs(providers, lat, lng);
    }

    @Transactional(readOnly = true)
    public ProviderDTO getById(Long id) {
        return providerRepository.findById(id)
                .map(provider -> toDTOs(List.of(provider), null, null).get(0))
                .orElseThrow(() -> new NotFoundException("Ce praticien n'existe pas ou n'est plus disponible."));
    }

    /**
     * Crée la fiche praticien du compte connecté. Le propriétaire est lu dans le JWT
     * (SecurityContextHolder -> UserRepository), jamais dans le corps de la requête.
     * Règles : compte de rôle PROVIDER, une seule fiche par compte, badge "vérifié" à false.
     */
    @Transactional
    public ProviderDTO create(ProviderCreateDTO dto) {
        User user = authenticatedUserService.requireUser();
        if (user.getRole() != User.Role.PROVIDER) {
            throw new ForbiddenException("Seuls les comptes praticiens peuvent publier une fiche.");
        }
        if (providerRepository.existsByUser_Id(user.getId())) {
            throw new ConflictException("Vous avez déjà une fiche praticien.");
        }

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setBio(dto.bio().trim());
        provider.setCategory(dto.category().trim());
        provider.setLocation(new Provider.Location(dto.location().latitude(), dto.location().longitude()));
        provider.setAvailability(dto.availability().stream()
                .map(this::toTimeSlot)
                .collect(Collectors.toCollection(HashSet::new)));
        provider.setImages(dto.images() == null ? new HashSet<>() : new HashSet<>(dto.images()));

        Provider saved = providerRepository.save(provider);
        cacheService.evictNearbyProviders();
        return toDTOs(List.of(saved), null, null).get(0);
    }

    /**
     * Praticiens à moins de 50 km ayant un créneau le jour demandé.
     * @param date "2026-10-24" (on en déduit le jour de la semaine) ou directement "SATURDAY"
     */
    @Transactional(readOnly = true)
    public List<ProviderDTO> getAvailableProviders(double lat, double lng, String date) {
        String dayOfWeek = resolveDayOfWeek(date);
        GeoUtils.BoundingBox box = GeoUtils.boundingBox(lat, lng, SEARCH_RADIUS_KM);
        List<Provider> providers = providerRepository.findAvailableProviders(
                lat, lng, SEARCH_RADIUS_KM,
                box.minLat(), box.maxLat(), box.minLng(), box.maxLng(),
                dayOfWeek);
        return toDTOs(providers, lat, lng);
    }

    /** Ce praticien appartient-il à ce compte ? (utilisé par le WebSocket) */
    @Transactional(readOnly = true)
    public boolean isOwner(Long providerId, String email) {
        return providerId != null && providerRepository.existsByIdAndUser_Email(providerId, email);
    }

    // ─── Conversions ─────────────────────────────────────────────────────────

    /**
     * Convertit une liste en calculant toutes les notes moyennes en UNE requête agrégée.
     * originLat/originLng : point de recherche pour la distance, ou null (page de détail).
     */
    List<ProviderDTO> toDTOs(List<Provider> providers, Double originLat, Double originLng) {
        if (providers.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = providers.stream().map(Provider::getId).collect(Collectors.toList());
        Map<Long, ReviewRepository.RatingSummary> ratings = reviewRepository.summarizeByProviderIds(ids)
                .stream()
                .collect(Collectors.toMap(ReviewRepository.RatingSummary::getProviderId, Function.identity()));

        return providers.stream()
                .map(p -> toDTO(p, ratings.get(p.getId()), originLat, originLng))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ProviderDTO toDTO(Provider provider, ReviewRepository.RatingSummary rating,
                              Double originLat, Double originLng) {
        ProviderDTO dto = new ProviderDTO();
        dto.setId(provider.getId());
        dto.setUserName(provider.getUser().getName());
        dto.setBio(provider.getBio());
        dto.setCategory(provider.getCategory());
        dto.setVerified(provider.isVerified());

        if (rating != null && rating.getTotal() != null && rating.getTotal() > 0) {
            dto.setRating(GeoUtils.round(rating.getAverage(), 1));
            dto.setReviewCount(rating.getTotal());
        } else {
            dto.setRating(null);
            dto.setReviewCount(0);
        }

        Provider.Location location = provider.getLocation();
        if (location != null) {
            // Position publique floutée à ~1 km ; la distance, elle, part des vraies coordonnées.
            dto.setLocation(new LocationDTO(
                    GeoUtils.round(location.getLatitude(), 2),
                    GeoUtils.round(location.getLongitude(), 2)));
            if (originLat != null && originLng != null) {
                double km = GeoUtils.distanceKm(originLat, originLng, location.getLatitude(), location.getLongitude());
                dto.setDistanceKm(GeoUtils.round(km, 1));
            }
        }

        dto.setAvailability(provider.getAvailability().stream()
                .sorted(Comparator.comparingInt((Provider.TimeSlot ts) -> dayOrder(ts.getDayOfWeek()))
                        .thenComparing(Provider.TimeSlot::getStartTime))
                .map(ts -> new TimeSlotDTO(ts.getDayOfWeek(), ts.getStartTime(), ts.getEndTime()))
                .collect(Collectors.toCollection(ArrayList::new)));
        // Copie dans un HashSet "neutre" : la PersistentSet d'Hibernate ne doit pas partir en cache.
        dto.setImages(new HashSet<>(provider.getImages()));
        return dto;
    }

    private Provider.TimeSlot toTimeSlot(TimeSlotDTO slot) {
        LocalTime start = LocalTime.parse(slot.startTime().trim(), INPUT_TIME);
        LocalTime end = LocalTime.parse(slot.endTime().trim(), INPUT_TIME);
        if (!end.isAfter(start)) {
            String day = DayOfWeek.valueOf(slot.dayOfWeek())
                    .getDisplayName(TextStyle.FULL, Locale.FRENCH);
            throw new BadRequestException("Le créneau du " + day + " doit se terminer après son heure de début.");
        }
        return new Provider.TimeSlot(slot.dayOfWeek(), start.format(STORED_TIME), end.format(STORED_TIME));
    }

    /** Lundi = 1 … dimanche = 7 ; valeur inconnue rangée en dernier plutôt que de planter. */
    private static int dayOrder(String dayOfWeek) {
        try {
            return DayOfWeek.valueOf(dayOfWeek).getValue();
        } catch (IllegalArgumentException | NullPointerException e) {
            return 8;
        }
    }

    private static String resolveDayOfWeek(String date) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Indiquez une date (ex. 2026-10-24).");
        }
        String value = date.trim();
        try {
            return LocalDate.parse(value).getDayOfWeek().name();
        } catch (DateTimeParseException ignored) {
            // Pas une date ISO : on tente un nom de jour ci-dessous.
        }
        try {
            return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Date invalide : utilisez le format AAAA-MM-JJ (ex. 2026-10-24) ou un jour comme MONDAY.");
        }
    }
}
