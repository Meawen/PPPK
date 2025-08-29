package com.pppk.patients.patients_infra.Repos;

import com.pppk.patients.patients_infra.Entities.MedicalHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalHistoryRepo extends JpaRepository<MedicalHistoryEntity, Long> {
}