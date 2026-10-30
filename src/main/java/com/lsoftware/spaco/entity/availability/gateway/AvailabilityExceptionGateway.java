package com.lsoftware.spaco.entity.availability.gateway;

import com.lsoftware.spaco.entity.availability.model.AvailabilityException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityExceptionGateway {
    AvailabilityException save(AvailabilityException exception);
    Optional<AvailabilityException> findById(Long id);
    /** Exceptions of that professional plus spa-wide ones (professional_id NULL). */
    List<AvailabilityException> findForProfessionalOnDate(Long professionalId, LocalDate date);
    List<AvailabilityException> findByDateRange(LocalDate from, LocalDate to);
    void deleteById(Long id);
}
