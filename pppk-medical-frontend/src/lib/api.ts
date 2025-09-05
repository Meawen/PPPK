import axios from 'axios'
import type {
    PatientResponse, CreatePatientRequest, UpdatePatientRequest,
    HistoryRequest, PrescriptionRequest, CreateExamRequest,
    ExamResponse, AttachmentRequest, AttachmentResponse, ExamTypeResponse
} from '../types'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    headers: { 'Content-Type': 'application/json' }
})

export const searchPatients = async (surname?: string, page = 0, size = 20) => {
    const r = await api.get('/api/patients', { params: { surname, page, size } });
    return toArray(r.data) as PatientResponse[];
};
export const getPatient = async (id: number) =>
    (await api.get<PatientResponse>(`/api/patients/${id}`)).data

export const createPatient = async (p: CreatePatientRequest) =>
    (await api.post<PatientResponse>('/api/patients', p)).data

export const updatePatient = async (id: number, p: UpdatePatientRequest) =>
    (await api.put<PatientResponse>(`/api/patients/${id}`, p)).data

export const deletePatient = async (id: number) =>
    (await api.delete(`/api/patients/${id}`)).data

export const byOib = async (oib: string) =>
    (await api.get<PatientResponse>(`/api/patients/by-oib/${oib}`)).data

export const exportPatientsCsvUrl = () => `${api.defaults.baseURL}/api/reporting/patients/export.csv`

// History / Prescriptions
export const addHistory = async (patientId: number, h: HistoryRequest) =>
    (await api.post(`/api/patients/${patientId}/history`, h)).data

export const prescribe = async (patientId: number, r: PrescriptionRequest) =>
    (await api.post(`/api/patients/${patientId}/prescriptions`, r)).data

const toOffsetIso = (v?: string) =>
    !v ? v : /[zZ]|[+-]\d{2}:\d{2}$/.test(v) ? v : new Date(v).toISOString();

export const createExam = async (e: CreateExamRequest) => {
    const payload = { ...e, occurredAt: toOffsetIso(e.occurredAt as any) };
    return (await api.post('/api/exams', payload)).data;
};



export const getExam = async (id: number) =>
    (await api.get<ExamResponse>(`/api/exams/${id}`)).data



export const addAttachment = async (examId: number, a: AttachmentRequest) => {
    const payload = { ...a, sizeBytes: a.sizeBytes ? Number(a.sizeBytes) : undefined };
    return (await api.post(`/api/exams/${examId}/attachments`, payload)).data;
};

const toArray = (x: any) =>
    Array.isArray(x) ? x :
        Array.isArray(x?.content) ? x.content :
            Array.isArray(x?.items) ? x.items : [];

export const listExams = async (patientId: number, page = 0, size = 20) => {
    const r = await api.get('/api/exams', { params: { patientId, page, size } });
    return toArray(r.data);
}

export const listExamTypes = async () => {
    const r = await api.get('/api/exams/types');
    return toArray(r.data);
}

export const listAttachments = async (examId: number) => {
    const r = await api.get(`/api/exams/${examId}/attachments`);
    return toArray(r.data);
}