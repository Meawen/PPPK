package com.pppk.patients.patients_domain.Model;

import com.pppk.patients.patients_domain.exception.DuplicateOibException;
import com.pppk.patients.patients_domain.port.OibUniquenessChecker;
import com.pppk.patients.patients_domain.vo.*;
import lombok.Getter;

import java.util.*;

public final class Patient {
    // getters
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
        this.id = id;
        this.oib = java.util.Objects.requireNonNull(oib, "oib");
        this.name = java.util.Objects.requireNonNull(name, "name");
        this.birthDate = java.util.Objects.requireNonNull(birthDate, "birthDate");
        this.sex = java.util.Objects.requireNonNull(sex, "sex");
    }
    public static Patient register(String oib, String first, String last,
                                   java.time.LocalDate dob, Sex sex,
                                   OibUniquenessChecker checker) {
        var oo = new Oib(oib);
        if (checker.exists(oo)) throw new DuplicateOibException(oo.value());
        return new Patient(null, oo, new PersonName(first,last), new BirthDate(dob), sex);
    }

    public void removeHistory(Long entryId) {
        history.removeIf(h -> java.util.Objects.equals(h.getId(), entryId));
    }

    public void updateHistory(Long entryId, String diseaseName, DateRange period) {
        for (int i = 0; i < history.size(); i++) {
            var h = history.get(i);
            if (java.util.Objects.equals(h.getId(), entryId)) {
                history.set(i, new MedicalHistoryEntry(
                        h.getId(),
                        diseaseName != null ? diseaseName : h.getDiseaseName(),
                        period != null ? period : h.getPeriod()
                ));
                return;
            }
        }
        throw new java.util.NoSuchElementException("history " + entryId + " not found");
    }

    public void addHistory(String diseaseName, DateRange period) {
        history.add(new MedicalHistoryEntry(null, diseaseName, period));
    }
    public void prescribe(String medication, String dosage, String instructions) {
        prescriptions.add(new Prescription(null, medication, dosage, instructions, null));
    }

    public void removePrescription(Long id) {
        prescriptions.removeIf(p -> java.util.Objects.equals(p.getId(), id));
    }

    public void updatePrescription(Long id, String medication, String dosage, String instructions) {
        for (int i = 0; i < prescriptions.size(); i++) {
            var p = prescriptions.get(i);
            if (java.util.Objects.equals(p.getId(), id)) {
                prescriptions.set(i, new Prescription(
                        p.getId(),
                        medication != null ? medication : p.getMedication(),
                        dosage != null ? dosage : p.getDosage(),
                        instructions != null ? instructions : p.getInstructions(),
                        p.getPrescribedAt()
                ));
                return;
            }
        }
        throw new java.util.NoSuchElementException("prescription " + id + " not found");
    }

    public static Patient rehydrate(Long id, Oib oib, PersonName name, BirthDate birthDate, Sex sex) {
        return new Patient(id, oib, name, birthDate, sex);
    }

    public Patient withName(PersonName name){
        var p = Patient.rehydrate(id, oib, name, birthDate, sex);
        p.mutableHistory().addAll(history); p.mutablePrescriptions().addAll(prescriptions); return p;
    }
    public Patient withBirthDate(BirthDate bd){
        var p = Patient.rehydrate(id, oib, name, bd, sex);
        p.mutableHistory().addAll(history); p.mutablePrescriptions().addAll(prescriptions); return p;
    }
    public Patient withSex(Sex s){
        var p = Patient.rehydrate(id, oib, name, birthDate, s);
        p.mutableHistory().addAll(history); p.mutablePrescriptions().addAll(prescriptions); return p;
    }

    public List<MedicalHistoryEntry> getHistory(){return Collections.unmodifiableList(history);}
    public List<Prescription> getPrescriptions(){return Collections.unmodifiableList(prescriptions);}


    public void setId(Long id){this.id=id;}
    public List<MedicalHistoryEntry> mutableHistory(){ return history; }
    public List<Prescription> mutablePrescriptions(){ return prescriptions;}

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Patient other)) return false;
        if (id != null && other.id != null) return java.util.Objects.equals(id, other.id);
        return java.util.Objects.equals(oib, other.oib);
    }
    @Override public int hashCode() { return id != null ? id.hashCode() : oib.hashCode(); }
}