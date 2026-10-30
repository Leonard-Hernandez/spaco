package com.lsoftware.spaco.infrastructure.config.db.mapper;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.infrastructure.config.db.schema.AppointmentEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.PatientEntity;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toDomain(PatientEntity e) {
        return new Patient(e.getId(), e.getFullName(), e.getPhoneE164(), e.getEmail(), e.getBirthDate(),
                e.getAuthcoSub(), e.getCreatedAt());
    }

    public PatientEntity toEntity(Patient p) {
        return PatientEntity.builder()
                .id(p.id()).fullName(p.fullName()).phoneE164(p.phoneE164()).email(p.email())
                .birthDate(p.birthDate()).authcoSub(p.authcoSub()).createdAt(p.createdAt())
                .build();
    }

    public Appointment toDomain(AppointmentEntity e) {
        return new Appointment(e.getId(), e.getTreatment().getId(), e.getTreatment().getName(),
                e.getProfessional().getId(), e.getProfessional().getDisplayName(), e.getPatient().getId(),
                e.getPatient().getFullName(), e.getPatient().getPhoneE164(), e.getStartAt(), e.getEndAt(),
                AppointmentStatus.valueOf(e.getStatus()), e.getPatientNote(), e.getManagementToken(),
                e.getDecidedAt(), e.getRejectReason(), e.getCreatedAt());
    }
}
