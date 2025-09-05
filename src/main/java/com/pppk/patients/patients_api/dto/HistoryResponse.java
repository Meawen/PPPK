package com.pppk.patients.patients_api.dto;

import java.time.LocalDate;

public record HistoryResponse(
        String diseaseName,
        LocalDate startDate,
        LocalDate endDate
) {}