package com.pppk.patients.patients_infra.Repos;

import com.pppk.patients.patients_infra.Entities.MedicalHistoryEntity;
import org.springframework.data.repository.Repository;

public interface MedicalHistoryRepo extends Repository<MedicalHistoryEntity, Long> {
}