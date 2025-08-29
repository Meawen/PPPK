package com.pppk.patients.patients_domain.Model;

import java.time.OffsetDateTime;

public record Prescription(Long id, String medication, String dosage, String instructions,
                           OffsetDateTime prescribedAt) {
    public Prescription(Long id, String medication, String dosage, String instructions, OffsetDateTime prescribedAt) {
        if (medication == null || medication.isBlank()) throw new IllegalArgumentException("medication");
        this.id = id;
        this.medication = medication;
        this.dosage = dosage;
        this.instructions = instructions;
        this.prescribedAt = prescribedAt == null ? OffsetDateTime.now() : prescribedAt;
    }
}