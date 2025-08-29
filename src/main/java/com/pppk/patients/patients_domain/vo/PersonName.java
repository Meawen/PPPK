package com.pppk.patients.patients_domain.vo;

public record PersonName(String first, String last) {
    public PersonName {
        if (first == null || first.isBlank()) throw new IllegalArgumentException("first");
        if (last == null || last.isBlank()) throw new IllegalArgumentException("last");
    }
}