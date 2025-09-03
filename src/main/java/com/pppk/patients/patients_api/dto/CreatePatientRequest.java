
package com.pppk.patients.patients_api.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record CreatePatientRequest(
        @Pattern(regexp="\\d{11}") String oib,
        @NotBlank String firstName, @NotBlank String lastName,
        @NotNull LocalDate birthDate, @Pattern(regexp="M|F|O") String sex) {}
