package com.pppk.patients.patients_infra.Mappers;

import org.mapstruct.*;
import com.pppk.patients.patients_domain.Model.MedicalHistoryEntry;
import com.pppk.patients.patients_domain.vo.DateRange;
import com.pppk.patients.patients_infra.Entities.MedicalHistoryEntity;

@Mapper(componentModel = "spring")
public interface MedicalHistoryMapper {

    @Mapping(target = "period", expression = "java(new DateRange(e.getStartDate(), e.getEndDate()))")
    MedicalHistoryEntry toDomain(MedicalHistoryEntity e);

    @Mappings({
            @Mapping(target = "patient",   ignore = true),
            @Mapping(target = "startDate", expression = "java(d.getPeriod().start())"),
            @Mapping(target = "endDate",   expression = "java(d.getPeriod().end())"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    MedicalHistoryEntity toEntity(MedicalHistoryEntry d);
}
