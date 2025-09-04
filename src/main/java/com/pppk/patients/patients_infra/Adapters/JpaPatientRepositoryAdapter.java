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
    private void syncChildren(PatientEntity e, Patient d) {

        if (e.getHistory() != null) e.getHistory().clear();
        else e.setHistory(new java.util.ArrayList<>());
        for (MedicalHistoryEntry h : d.getHistory()) {
            MedicalHistoryEntity he = historyMapper.toEntity(h);
            he.setPatient(e);
            e.getHistory().add(he);
        }

        if (e.getPrescriptions() != null) e.getPrescriptions().clear();
        else e.setPrescriptions(new java.util.ArrayList<>());
        for (Prescription pr : d.getPrescriptions()) {
            PrescriptionEntity pe = prescriptionMapper.toEntity(pr);
            pe.setPatient(e);
            e.getPrescriptions().add(pe);
        }
    }

    @Override
    @Transactional

    public Patient save(Patient d) {
        if (d.getId() == null) {
            PatientEntity e = patientMapper.toEntity(d);
            syncChildren(e, d);
            return patientMapper.toDomain(patientRepo.save(e));
        }

        PatientEntity e = patientRepo.findById(d.getId())
                .orElseThrow(() -> new PatientNotFoundException(d.getId()));

        e.setOib(d.getOib().value());
        e.setFirstName(d.getName().first());
        e.setLastName(d.getName().last());
        e.setBirthDate(d.getBirthDate().value());
        e.setSex(com.pppk.patients.patients_infra.Enums.Sex.valueOf(d.getSex().name()));

        e.getHistory().clear();
        for (var h : d.getHistory()) {
            var he = historyMapper.toEntity(h);
            he.setPatient(e);
            e.getHistory().add(he);
        }

        e.getPrescriptions().clear();
        for (var pr : d.getPrescriptions()) {
            var pe = prescriptionMapper.toEntity(pr);
            pe.setPatient(e);
            e.getPrescriptions().add(pe);
        }

        return patientMapper.toDomain(patientRepo.save(e)); // or return mapper.toDomain(e)
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!patientRepo.existsById(id)) throw new PatientNotFoundException(id);
        patientRepo.deleteById(id);
    }
}