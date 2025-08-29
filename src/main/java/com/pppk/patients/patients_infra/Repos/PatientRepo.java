package com.pppk.patients.patients_infra.Repos;

import com.pppk.patients.patients_infra.Entities.PatientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepo extends JpaRepository<PatientEntity, Long> {
    Optional<PatientEntity> findByOib(String oib);
    boolean existsByOib(String oib);
    Page<PatientEntity> findByLastNameContainingIgnoreCase(String last, Pageable p);
}