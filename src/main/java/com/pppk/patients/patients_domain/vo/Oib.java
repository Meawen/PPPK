package com.pppk.patients.patients_domain.vo;

public record Oib(String value) {
    public Oib {
        if (value == null || !value.matches("\\d{11}")) throw new IllegalArgumentException("Invalid OIB");
    }
    @Override public String toString() { return value; }
}