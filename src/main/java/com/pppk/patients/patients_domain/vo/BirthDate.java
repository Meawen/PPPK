package com.pppk.patients.patients_domain.vo;

import java.time.LocalDate;
public record BirthDate(LocalDate value) {
    public BirthDate {
        if (value == null || value.isAfter(LocalDate.now())) throw new IllegalArgumentException("birthDate");
    }
}