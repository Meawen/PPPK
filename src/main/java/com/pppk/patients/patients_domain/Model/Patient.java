package com.pppk.patients.patients_domain.Model;// Patient.java

import com.pppk.patients.patients_domain.exception.DuplicateOibException;
import com.pppk.patients.patients_domain.port.OibUniquenessChecker;
import com.pppk.patients.patients_domain.vo.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

public final class Patient {
    @Setter
    @Getter
    private Long id;
    @Getter
    private final Oib oib;
    @Getter
    private final PersonName name;
    @Getter
    private final BirthDate birthDate;
    @Getter
    private final Sex sex;
    private final List<MedicalHistoryEntry> history = new ArrayList<>();
    private final List<Prescription> prescriptions = new ArrayList<>();

    private Patient(Long id, Oib oib, PersonName name, BirthDate birthDate, Sex sex) {
        this.id=id; this.oib=oib; this.name=name; this.birthDate=birthDate; this.sex=sex;
    }
    public static Patient register(String oib, String first, String last,
                                   java.time.LocalDate dob, Sex sex,
                                   OibUniquenessChecker checker) {
        var oo = new Oib(oib);
        if (checker.exists(oo)) throw new DuplicateOibException(oo.value());
        return new Patient(null, oo, new PersonName(first,last), new BirthDate(dob), sex);
    }

    public void addHistory(String diseaseName, DateRange period) {
        history.add(new MedicalHistoryEntry(null, diseaseName, period));
    }
    public void prescribe(String medication, String dosage, String instructions) {
        prescriptions.add(new Prescription(null, medication, dosage, instructions, null));
    }


    public List<MedicalHistoryEntry> getHistory(){return Collections.unmodifiableList(history);}
    public List<Prescription> getPrescriptions(){return Collections.unmodifiableList(prescriptions);}


    public List<MedicalHistoryEntry> mutableHistory(){ return history; }
    public List<Prescription> mutablePrescriptions(){ return prescriptions; }
}
