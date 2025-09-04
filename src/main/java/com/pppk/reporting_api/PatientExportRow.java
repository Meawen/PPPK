package com.pppk.reporting_api;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PatientExportRow(
        Long id, String firstName, String lastName, String oib,
        LocalDate birthDate, String sex, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}
