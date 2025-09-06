package com.pppk.patients.patients_api;

import com.pppk.patients.patients_api.dto.*;
import com.pppk.patients.patients_domain.Model.*;
import com.pppk.patients.patients_domain.vo.*;
import com.pppk.patients.patients_domain.port.*;
import com.pppk.patients.patients_infra.Entities.MedicalHistoryEntity;
import com.pppk.patients.patients_infra.Entities.PatientEntity;
import com.pppk.patients.patients_infra.Entities.PrescriptionEntity;
import com.pppk.patients.patients_infra.Mappers.MedicalHistoryMapper;
import com.pppk.patients.patients_infra.Mappers.PrescriptionMapper;
import com.pppk.patients.patients_infra.Repos.MedicalHistoryRepo;
import com.pppk.patients.patients_infra.Repos.PrescriptionRepo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PatientAppService {
    private final PatientRepository repo;
    private final MedicalHistoryRepo historyRepo;
    private final PrescriptionRepo prescriptionRepo;
    private final MedicalHistoryMapper historyMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final OibUniquenessChecker oibCheck;
    @PersistenceContext
    private EntityManager entityManager;
    @Transactional
    public PatientResponse create(CreatePatientRequest r){
        var p = Patient.register(r.oib(), r.firstName(), r.lastName(), r.birthDate(), Sex.valueOf(r.sex()), oibCheck);
        return toResp(repo.save(p));
    }

    @Transactional(readOnly=true)
    public PatientResponse get(Long id){ return repo.byId(id).map(this::toResp).orElseThrow(); }

    @Transactional(readOnly=true)
    public Page<PatientResponse> search(String surname, Pageable p){ return repo.byLastName(surname==null?"":surname, p).map(this::toResp); }

    @Transactional(readOnly = true)
    public PatientResponse byOib(String oib) {
        return repo.byOib(new Oib(oib)).map(this::toResp).orElseThrow();
    }
    @Transactional
    public PatientResponse update(Long id, UpdatePatientRequest r){
        var p = repo.byId(id).orElseThrow();
        if (r.firstName()!=null || r.lastName()!=null)
            p = p.withName(new PersonName(r.firstName()!=null? r.firstName():p.getName().first(),
                    r.lastName()!=null? r.lastName():p.getName().last()));
        if (r.birthDate()!=null) p = p.withBirthDate(new BirthDate(r.birthDate()));
        if (r.sex()!=null)       p = p.withSex(Sex.valueOf(r.sex()));
        return toResp(repo.save(p));
    }

    @Transactional public void delete(Long id){ repo.delete(id); }

    @Transactional
    public void addHistory(Long patientId, HistoryRequest r) {
        var e = new MedicalHistoryEntity();
        e.setDiseaseName(r.diseaseName());
        e.setStartDate(r.startDate());
        e.setEndDate(r.endDate());

        var pe = entityManager.getReference(PatientEntity.class, patientId);
        e.setPatient(pe);

        historyRepo.save(e);
    }

    @Transactional
    public void prescribe(Long patientId, PrescriptionRequest r) {
        var e = new PrescriptionEntity();
        e.setMedication(r.medication());
        e.setDosage(r.dosage());
        e.setInstructions(r.instructions());

        var pe = entityManager.getReference(PatientEntity.class, patientId); // <-- replace the stub
        e.setPatient(pe);

        prescriptionRepo.save(e);
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(Long patientId) {
        return historyRepo.findAllByPatient_IdOrderByStartDateDesc(patientId)
                .stream()
                .map(h -> new HistoryResponse(
                        h.getDiseaseName(),
                        h.getStartDate(),
                        h.getEndDate()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> prescriptions(Long patientId) {
        return prescriptionRepo.findAllByPatient_IdOrderByPrescribedAtDesc(patientId)
                .stream()
                .map(p -> new PrescriptionResponse(

                        p.getMedication(),
                        p.getDosage(),
                        p.getInstructions()
                ))
                .toList();
    }
    private PatientResponse toResp(Patient p){
        return new PatientResponse(p.getId(), p.getOib().value(), p.getName().first(), p.getName().last(),
                p.getBirthDate().value(), p.getSex().name());
    }
}
