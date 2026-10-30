package com.lsoftware.spaco.infrastructure.appointment.gateway;

import com.lsoftware.spaco.entity.appointment.exception.SlotTakenException;
import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.infrastructure.config.db.mapper.PatientMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaAppointmentRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaPatientRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaProfessionalRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.AppointmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentJpaGateway implements AppointmentGateway {

    private final JpaAppointmentRepository repo;
    private final JpaTreatmentRepository treatments;
    private final JpaProfessionalRepository professionals;
    private final JpaPatientRepository patients;
    private final PatientMapper mapper;

    @Override
    public Appointment save(Appointment a) {
        AppointmentEntity entity = a.id() != null ? repo.findOneById(a.id()).orElseThrow() : new AppointmentEntity();
        entity.setTreatment(treatments.findOneById(a.treatmentId()).orElseThrow());
        entity.setProfessional(professionals.findOneById(a.professionalId()).orElseThrow());
        entity.setPatient(patients.findOneById(a.patientId()).orElseThrow());
        entity.setStartAt(a.startAt());
        entity.setEndAt(a.endAt());
        entity.setStatus(a.status().name());
        entity.setPatientNote(a.patientNote());
        entity.setDecidedAt(a.decidedAt());
        entity.setRejectReason(a.rejectReason());
        try {
            return mapper.toDomain(repo.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("no_overlap_professional")) {
                throw new SlotTakenException();
            }
            throw e;
        }
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Appointment> findByManagementToken(UUID token) {
        return repo.findByManagementToken(token).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findActiveOverlapping(Long professionalId, OffsetDateTime start, OffsetDateTime end) {
        return repo.findActiveOverlapping(professionalId, start, end).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findByDateRange(OffsetDateTime from, OffsetDateTime to, AppointmentStatus status,
                                             Long professionalId) {
        return repo.findByDateRange(from, to, status != null ? status.name() : null, professionalId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findByPatient(Long patientId) {
        return repo.findByPatientIdOrderByStartAtDesc(patientId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Appointment> findByStatus(AppointmentStatus status) {
        return repo.findByStatusOrderByStartAtAsc(status.name()).stream().map(mapper::toDomain).toList();
    }
}
