package com.pppk.patients.patients_api.dto;

import jakarta.validation.constraints.NotBlank;

public record PrescriptionRequest(@NotBlank String medication, String dosage, String instructions) {}
