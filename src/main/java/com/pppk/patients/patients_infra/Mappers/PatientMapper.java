package com.pppk.patients.patients_infra.Mappers;

import org.mapstruct.*;
import com.pppk.patients.patients_domain.Model.*;
import com.pppk.patients.patients_domain.vo.*;
import com.pppk.patients.patients_infra.Entities.*;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = { MedicalHistoryMapper.class, PrescriptionMapper.class })
public interface PatientMapper {

    // ---------- entity -> domain ----------
    @BeanMapping(ignoreByDefault = true)
    Patient toDomain(PatientEntity e);

    @ObjectFactory
    default Patient makePatient(PatientEntity e) {
        return Patient.rehydrate(
                e.getId(),
                new Oib(e.getOib()),
                new PersonName(e.getFirstName(), e.getLastName()),
                new BirthDate(e.getBirthDate()),
                Sex.valueOf(String.valueOf(e.getSex()))        // if entity keeps sex as String
        );
    }

    @AfterMapping
    default void fillCollections(PatientEntity e, @MappingTarget Patient target,
                                 MedicalHistoryMapper hm, PrescriptionMapper pm) {
        if (e.getHistory() != null) {
            target.mutableHistory().addAll(
                    e.getHistory().stream().map(hm::toDomain).toList()
            );
        }
        if (e.getPrescriptions() != null) {
            target.mutablePrescriptions().addAll(
                    e.getPrescriptions().stream().map(pm::toDomain).toList()
            );
        }
    }

    // ---------- domain -> entity ----------
    @Mappings({
            @Mapping(target = "id",         source = "id"),
            @Mapping(target = "oib",        expression = "java(d.getOib().value())"),
            @Mapping(target = "firstName",  expression = "java(d.getName().first())"),
            @Mapping(target = "lastName",   expression = "java(d.getName().last())"),
            @Mapping(target = "birthDate",  expression = "java(d.getBirthDate().value())"),
            @Mapping(target = "sex",
                    expression = "java(com.pppk.patients.patients_infra.Enums.Sex.valueOf(d.getSex().name()))") ,
            @Mapping(target = "history",    ignore = true),
            @Mapping(target = "prescriptions", ignore = true),
            @Mapping(target = "createdAt",  ignore = true),
            @Mapping(target = "updatedAt",  ignore = true)
    })
    PatientEntity toEntity(Patient d);
}
