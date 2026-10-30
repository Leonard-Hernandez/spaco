package com.lsoftware.spaco.entity.professional.gateway;

import com.lsoftware.spaco.entity.professional.model.Professional;

import java.util.List;
import java.util.Optional;

public interface ProfessionalGateway {
    Professional save(Professional professional);
    Optional<Professional> findById(Long id);
    List<Professional> findAll();
    List<Professional> findAllActive();
}
