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
        if (e.getHistories() == null) e.setHistories(new java.util.ArrayList<>());
        if (e.getPrescriptions() == null) e.setPrescriptions(new java.util.ArrayList<>());

        var histById = new java.util.HashMap<Long, MedicalHistoryEntity>();
        for (var he : e.getHistories()) if (he.getId() != null) histById.put(he.getId(), he);

        var rxById = new java.util.HashMap<Long, PrescriptionEntity>();
        for (var pe : e.getPrescriptions()) if (pe.getId() != null) rxById.put(pe.getId(), pe);

        for (var h : d.getHistory()) {
            if (h.getId() == null) {
                var he = historyMapper.toEntity(h);
                he.setPatient(e);
                e.getHistories().add(he);
            } else {
                var he = histById.get(h.getId());
                if (he != null) {
                    he.setDiseaseName(h.getDiseaseName());
                    he.setStartDate(h.getPeriod().start());
                    he.setEndDate(h.getPeriod().end());
                }
            }
        }

        for (var pr : d.getPrescriptions()) {
            if (pr.getId() == null) {
                var pe = prescriptionMapper.toEntity(pr);
                pe.setPatient(e);
                e.getPrescriptions().add(pe);
            } else {
                var pe = rxById.get(pr.getId());
                if (pe != null) {
                    pe.setMedication(pr.getMedication());
                    pe.setDosage(pr.getDosage());
                    pe.setInstructions(pr.getInstructions());
                }
            }
        }
    }


    @Override
    @Transactional
    public Patient save(Patient d) {
        if (d.getId() == null) {
            var e = patientMapper.toEntity(d);
            syncChildren(e, d);                      // adds all new children
            return patientMapper.toDomain(patientRepo.save(e));
        }
        var e = patientRepo.findById(d.getId())
                .orElseThrow(() -> new PatientNotFoundException(d.getId()));

        e.setOib(d.getOib().value());
        e.setFirstName(d.getName().first());
        e.setLastName(d.getName().last());
        e.setBirthDate(d.getBirthDate().value());
        e.setSex(com.pppk.patients.patients_infra.Enums.Sex.valueOf(d.getSex().name()));

        syncChildren(e, d);                          // merge, no deletes
        return patientMapper.toDomain(patientRepo.save(e));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!patientRepo.existsById(id)) throw new PatientNotFoundException(id);
        patientRepo.deleteById(id);
    }
}