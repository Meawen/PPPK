package com.pppk.patients.patients_domain.Model;

import lombok.Getter;

import java.time.OffsetDateTime;
@Getter
public final class Prescription {
    private Long id;
    private final String medication;
    private final String dosage;
    private final String instructions;
    private final OffsetDateTime prescribedAt;
    public Prescription(Long id, String medication, String dosage, String instructions, OffsetDateTime at) {
        if (medication == null || medication.isBlank()) throw new IllegalArgumentException("medication");
        this.id=id; this.medication=medication; this.dosage=dosage; this.instructions=instructions;
        this.prescribedAt = at == null ? OffsetDateTime.now() : at;
    }
}