package com.pppk.patients.patients_api.dto;

import java.time.LocalDate;

public record PatientResponse(Long id, String oib, String firstName,
                              String lastName, LocalDate birthDate, String sex) {}
