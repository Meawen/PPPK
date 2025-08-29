package com.pppk.patients.patients_infra.Repos;

import com.pppk.patients.patients_infra.Entities.PrescriptionEntity;
import org.springframework.data.repository.Repository;

public interface PrescriptionRepo extends Repository<PrescriptionEntity, Long> {
}