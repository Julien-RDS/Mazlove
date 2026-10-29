package com.mazlove.backend.service;

import com.mazlove.backend.dto.ServiceItemDTO;
import com.mazlove.backend.exception.ForbiddenException;
import com.mazlove.backend.exception.NotFoundException;
import com.mazlove.backend.models.Provider;
import com.mazlove.backend.models.ServiceItem;
import com.mazlove.backend.repository.ProviderRepository;
import com.mazlove.backend.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ServiceItemService {
    private final ServiceItemRepository items;
    private final ProviderRepository providers;
    private final AuthenticatedUserService users;

    public ServiceItemService(ServiceItemRepository items, ProviderRepository providers, AuthenticatedUserService users) {
        this.items = items;
        this.providers = providers;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ServiceItemDTO> list(Long providerId) {
        if (!providers.existsById(providerId)) throw new NotFoundException("Ce praticien n'existe pas.");
        return items.findByProvider_IdOrderByIdAsc(providerId).stream().map(ServiceItemDTO::from).toList();
    }

    @Transactional
    public ServiceItemDTO create(Long providerId, ServiceItemDTO dto) {
        Provider provider = ownedProvider(providerId);
        ServiceItem item = new ServiceItem();
        item.setProvider(provider);
        item.setName(dto.name().trim());
        item.setDescription(dto.description().trim());
        item.setPrice(dto.price());
        item.setDuration(dto.duration());
        return ServiceItemDTO.from(items.save(item));
    }

    @Transactional
    public void delete(Long providerId, Long id) {
        ownedProvider(providerId);
        ServiceItem item = items.findByIdAndProvider_Id(id, providerId)
                .orElseThrow(() -> new NotFoundException("Cette prestation n'existe pas."));
        items.delete(item);
    }

    private Provider ownedProvider(Long providerId) {
        Provider provider = providers.findById(providerId)
                .orElseThrow(() -> new NotFoundException("Ce praticien n'existe pas."));
        if (!provider.getUser().getId().equals(users.requireUser().getId()))
            throw new ForbiddenException("Vous ne pouvez gérer que vos propres prestations.");
        return provider;
    }
}
