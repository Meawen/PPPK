package com.pppk.patients.patients_api;

import com.pppk.patients.patients_api.dto.*;
import com.pppk.patients.patients_domain.Model.*;
import com.pppk.patients.patients_domain.vo.*;
import com.pppk.patients.patients_domain.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class PatientAppService {
    private final PatientRepository repo; private final OibUniquenessChecker oibCheck;

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

    @Transactional public void addHistory(Long id, HistoryRequest r){
        var p = repo.byId(id).orElseThrow();
        p.addHistory(r.diseaseName(), new DateRange(r.startDate(), r.endDate()));
        repo.save(p);
    }

    @Transactional public void prescribe(Long id, PrescriptionRequest r){
        var p = repo.byId(id).orElseThrow();
        p.prescribe(r.medication(), r.dosage(), r.instructions());
        repo.save(p);
    }

    private PatientResponse toResp(Patient p){
        return new PatientResponse(p.getId(), p.getOib().value(), p.getName().first(), p.getName().last(),
                p.getBirthDate().value(), p.getSex().name());
    }
}
