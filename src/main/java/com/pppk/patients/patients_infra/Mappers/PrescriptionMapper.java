package com.pppk.patients.patients_infra.Mappers;

import org.mapstruct.*;
import com.pppk.patients.patients_domain.Model.Prescription;
import com.pppk.patients.patients_infra.Entities.PrescriptionEntity;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {
    Prescription toDomain(PrescriptionEntity e);

    @Mappings({
            @Mapping(target = "patient",   ignore = true),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    PrescriptionEntity toEntity(Prescription d);
}
