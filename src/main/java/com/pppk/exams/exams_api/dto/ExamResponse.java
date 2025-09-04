package com.pppk.exams.exams_api.dto;

import java.time.OffsetDateTime;

public record ExamResponse(Long id, Long patientId, OffsetDateTime occurredAt, String examType, String notes) {}

