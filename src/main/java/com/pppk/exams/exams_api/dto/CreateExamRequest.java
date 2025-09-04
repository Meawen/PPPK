package com.pppk.exams.exams_api.dto;

import java.time.OffsetDateTime;

public record CreateExamRequest(Long patientId, OffsetDateTime occurredAt, String examType, String notes) {}

