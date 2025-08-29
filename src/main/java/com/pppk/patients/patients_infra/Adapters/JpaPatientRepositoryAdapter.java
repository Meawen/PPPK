package com.pppk.patients.patients_infra.Adapters;

import com.pppk.patients.patients_domain.Model.MedicalHistoryEntry;
import com.pppk.patients.patients_domain.Model.Patient;
import com.pppk.patients.patients_domain.Model.Prescription;
import com.pppk.patients.patients_domain.exception.PatientNotFoundException;
import com.pppk.patients.patients_domain.port.OibUniquenessChecker;
import com.pppk.patients.patients_domain.port.PatientRepository;
import com.pppk.patients.patients_domain.vo.Oib;
import com.pppk.patients.patients_infra.Entities.MedicalHistoryEntity;
import com.pppk.patients.patients_infra.Entities.PatientEntity;
import com.pppk.patients.patients_infra.Entities.PrescriptionEntity;
import com.pppk.patients.patients_infra.Mappers.MedicalHistoryMapper;
import com.pppk.patients.patients_infra.Mappers.PatientMapper;
import com.pppk.patients.patients_infra.Mappers.PrescriptionMapper;
import com.pppk.patients.patients_infra.Repos.MedicalHistoryRepo;
import com.pppk.patients.patients_infra.Repos.PatientRepo;
import com.pppk.patients.patients_infra.Repos.PrescriptionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaPatientRepositoryAdapter implements PatientRepository, OibUniquenessChecker {

    private final PatientRepo patientRepo;
    private final PatientMapper patientMapper;
    private final MedicalHistoryMapper historyMapper;
    private final PrescriptionMapper prescriptionMapper;

    @Override
    public boolean exists(Oib oib) { return patientRepo.existsByOib(oib.value()); }

    @Override
    public java.util.Optional<Patient> byId(Long id) {
        return patientRepo.findById(id).map(patientMapper::toDomain);
    }

    @Override
    public java.util.Optional<Patient> byOib(Oib oib) {
        return patientRepo.findByOib(oib.value()).map(patientMapper::toDomain);
    }

    @Override
    public Page<Patient> byLastName(String q, Pageable p) {
        return patientRepo.findByLastNameContainingIgnoreCase(q == null ? "" : q, p)
                .map(patientMapper::toDomain);
    }

    @Override
    @Transactional
    public Patient save(Patient d) {
        PatientEntity e = patientMapper.toEntity(d);

        e.getHistory().clear();
        for (MedicalHistoryEntry he : d.getHistory()) {
            MedicalHistoryEntity hee = historyMapper.toEntity(he);
            hee.setPatient(e);
            e.getHistory().add(hee);
        }
        e.getPrescriptions().clear();
        for (Prescription pr : d.getPrescriptions()) {
            PrescriptionEntity pre = prescriptionMapper.toEntity(pr);
            pre.setPatient(e);
            e.getPrescriptions().add(pre);
        }

        PatientEntity saved = patientRepo.save(e);
        return patientMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!patientRepo.existsById(id)) throw new PatientNotFoundException(id);
        patientRepo.deleteById(id);
    }
}