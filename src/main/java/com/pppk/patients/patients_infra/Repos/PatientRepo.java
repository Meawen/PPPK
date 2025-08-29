package com.pppk.patients.patients_infra.Repos;

import com.pppk.patients.patients_infra.Entities.PatientEntity;
import org.springframework.data.repository.Repository;

public interface PatientRepo extends Repository<PatientEntity, Long> { }