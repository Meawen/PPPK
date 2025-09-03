package com.pppk.patients.patients_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record HistoryRequest(@NotBlank String diseaseName, @NotNull LocalDate startDate, LocalDate endDate) {}
