package com.lsoftware.spaco.infrastructure.config.db.mapper;

import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.ExceptionType;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.infrastructure.config.db.schema.AvailabilityExceptionEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.AvailabilityRuleEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.ProfessionalEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class ScheduleMapper {

    public Professional toDomain(ProfessionalEntity e) {
        return new Professional(e.getId(), e.getDisplayName(), e.getTitle(), e.getBio(), e.getPhotoPath(),
                e.getMemberId(), e.isActive());
    }

    public ProfessionalEntity toEntity(Professional p) {
        return ProfessionalEntity.builder()
                .id(p.id()).displayName(p.displayName()).title(p.title()).bio(p.bio()).photoPath(p.photoPath())
                .memberId(p.memberId()).active(p.active())
                .build();
    }

    public AvailabilityRule toDomain(AvailabilityRuleEntity e) {
        return new AvailabilityRule(e.getId(), e.getProfessionalId(), DayOfWeek.of(e.getWeekday()), e.getStartTime(),
                e.getEndTime(), e.isActive());
    }

    public AvailabilityRuleEntity toEntity(AvailabilityRule r) {
        return AvailabilityRuleEntity.builder()
                .id(r.id()).professionalId(r.professionalId()).weekday((short) r.weekday().getValue())
                .startTime(r.startTime()).endTime(r.endTime()).active(r.active())
                .build();
    }

    public AvailabilityException toDomain(AvailabilityExceptionEntity e) {
        return new AvailabilityException(e.getId(), e.getProfessionalId(), e.getDate(), e.getStartTime(),
                e.getEndTime(), ExceptionType.valueOf(e.getType()), e.getNote());
    }

    public AvailabilityExceptionEntity toEntity(AvailabilityException x) {
        return AvailabilityExceptionEntity.builder()
                .id(x.id()).professionalId(x.professionalId()).date(x.date()).startTime(x.startTime())
                .endTime(x.endTime()).type(x.type().name()).note(x.note())
                .build();
    }
}
