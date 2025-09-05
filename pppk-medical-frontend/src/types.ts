export type PatientResponse = {
    id?: number
    oib?: string
    firstName?: string
    lastName?: string
    birthDate?: string
    sex?: string
}

export type CreatePatientRequest = {
    oib?: string
    firstName?: string
    lastName?: string
    birthDate?: string
    sex?: string
}

export type UpdatePatientRequest = Partial<CreatePatientRequest>

export type HistoryRequest = {
    diseaseName?: string
    startDate?: string
    endDate?: string
}

export type PrescriptionRequest = {
    medication?: string
    dosage?: string
    instructions?: string
}

export type CreateExamRequest = {
    patientId?: number
    occurredAt?: string
    examType?: string
    notes?: string
}

export type AttachmentRequest = {
    objectKey?: string
    contentType?: string
    sizeBytes?: number
    sha256Hex?: string
}

export type ExamResponse = {
    id?: number
    patientId?: number
    occurredAt?: string
    examType?: string
    notes?: string
}

export type AttachmentResponse = {
    id?: number
    objectKey?: string
    contentType?: string
    sizeBytes?: number
    sha256Hex?: string
}

export type ExamTypeResponse = { code?: string; name?: string }
