package com.pppk.exams.exams_api.dto;

public record AttachmentResponse(Long id, String objectKey, String contentType, Long sizeBytes, String sha256Hex) {}

