package com.pppk.patients.patients_api.dto;

public record PrescriptionResponse(
        String medication,
        String dosage,
        String instructions
) {}