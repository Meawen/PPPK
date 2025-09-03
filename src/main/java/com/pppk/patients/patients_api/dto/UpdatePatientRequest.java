package com.pppk.patients.patients_api.dto;
import java.time.LocalDate;
public record UpdatePatientRequest(String firstName,
                                   String lastName,
                                   LocalDate birthDate,
                                   String sex) {}
