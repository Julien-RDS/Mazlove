package com.mazlove.backend.repository;

import com.mazlove.backend.models.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findByProvider_IdOrderByIdAsc(Long providerId);
    Optional<ServiceItem> findByIdAndProvider_Id(Long id, Long providerId);
}
