package com.mazlove.backend.service;

import com.mazlove.backend.dto.ProviderCreateDTO;
import com.mazlove.backend.dto.ProviderDTO;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.repository.ProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProviderService {
    private final ProviderRepository providerRepository;

    @Autowired
    public ProviderService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderDTO> findNearby(double lat, double lng, int page, int size) {
        return providerRepository.findNearby(lat, lng, PageRequest.of(page, size))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProviderDTO getById(String id) {
        return providerRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
    }

    @Transactional
    public ProviderDTO create(ProviderCreateDTO dto) {
        Provider provider = new Provider();
        provider.setName(dto.name());
        provider.setDescription(dto.description());
        provider.setCategory(dto.category());
        provider.setLocation(new Provider.Location(dto.location().latitude(), dto.location().longitude()));
        
        // Convertir les TimeSlotDTO en TimeSlot
        List<Provider.TimeSlot> availability = dto.availability().stream()
            .map(ts -> new Provider.TimeSlot(ts.dayOfWeek(), ts.startTime(), ts.endTime()))
            .collect(Collectors.toList());
        provider.setAvailability(availability);
        
        // Sauvegarder les images
        provider.setImages(dto.images());
        
        Provider saved = providerRepository.save(provider);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ProviderDTO> getAvailableProviders(double lat, double lng, String date) {
        return providerRepository.findAvailableProviders(lat, lng, date)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ProviderDTO toDTO(Provider provider) {
        ProviderDTO dto = new ProviderDTO();
        dto.setId(provider.getId());
        dto.setName(provider.getName());
        dto.setDescription(provider.getDescription());
        dto.setCategory(provider.getCategory());
        dto.setRating(provider.getRating());
        dto.setVerified(provider.isVerified());
        dto.setLocation(new ProviderDTO.LocationDTO(
            provider.getLocation().getLatitude(),
            provider.getLocation().getLongitude()
        ));
        dto.setAvailability(provider.getAvailability().stream()
            .map(ts -> new ProviderDTO.TimeSlotDTO(
                ts.getDayOfWeek(),
                ts.getStartTime(),
                ts.getEndTime()
            ))
            .collect(Collectors.toList()));
        dto.setImages(provider.getImages());
        return dto;
    }
}
