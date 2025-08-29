package com.pppk.patients.patients_infra.Repos;


import com.pppk.patients.patients_infra.Entities.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionRepo extends JpaRepository<PrescriptionEntity, Long> {
}