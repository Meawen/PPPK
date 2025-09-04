package com.pppk.exams.exams_api.dto;

public record AttachmentRequest(String objectKey, String contentType, Long sizeBytes, String sha256Hex) {}

