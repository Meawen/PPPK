package com.pppk.patients.patients_domain.port;

import com.pppk.patients.patients_domain.Model.Patient;
import com.pppk.patients.patients_domain.vo.Oib;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PatientRepository {
    Optional<Patient> byId(Long id);
    Optional<Patient> byOib(Oib oib);
    Page<Patient> byLastName(String q, Pageable p);
    Patient save(Patient p);
    void delete(Long id);
}