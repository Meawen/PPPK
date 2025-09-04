package com.pppk.exams.exams_api;

import com.pppk.exams.exams_api.dto.*;
import com.pppk.exams.exams_infra.entities.ExamAttachmentEntity;
import com.pppk.exams.exams_infra.entities.ExamEntity;
import com.pppk.exams.exams_infra.repos.ExamAttachmentRepo;
import com.pppk.exams.exams_infra.repos.ExamRepo;
import com.pppk.exams.exams_infra.repos.ExamTypeRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamAppService {
    private final ExamRepo exams;
    private final ExamTypeRepo types;
    private final ExamAttachmentRepo atts;

    public ExamResponse create(CreateExamRequest r){
        var t = types.findById(r.examType()).orElseThrow();
        var e = new ExamEntity();
        e.setPatientId(r.patientId()); e.setOccurredAt(r.occurredAt()); e.setExamType(t); e.setNotes(r.notes());
        e = exams.save(e);
        return new ExamResponse(e.getId(), e.getPatientId(), e.getOccurredAt(), e.getExamType().getCode(), e.getNotes());
    }

    @Transactional(readOnly=true)
    public ExamResponse get(Long id){
        var e = exams.findById(id).orElseThrow();
        return new ExamResponse(e.getId(), e.getPatientId(), e.getOccurredAt(), e.getExamType().getCode(), e.getNotes());
    }

    @Transactional(readOnly=true)
    public Page<ExamResponse> byPatient(Long patientId, Pageable p){
        return exams.findByPatientId(patientId, p)
                .map(e -> new ExamResponse(e.getId(), e.getPatientId(), e.getOccurredAt(), e.getExamType().getCode(), e.getNotes()));
    }

    public AttachmentResponse addAttachment(Long examId, AttachmentRequest r){
        var e = exams.findById(examId).orElseThrow();
        var a = new ExamAttachmentEntity();
        a.setExam(e); a.setObjectKey(r.objectKey()); a.setContentType(r.contentType());
        a.setSizeBytes(r.sizeBytes()); a.setSha256Hex(r.sha256Hex());
        a = atts.save(a);
        return new AttachmentResponse(a.getId(), a.getObjectKey(), a.getContentType(), a.getSizeBytes(), a.getSha256Hex());
    }

    @Transactional(readOnly=true)
    public List<AttachmentResponse> listAttachments(Long examId){
        return atts.findByExamId(examId).stream()
                .map(a -> new AttachmentResponse(a.getId(), a.getObjectKey(), a.getContentType(), a.getSizeBytes(), a.getSha256Hex()))
                .toList();
    }

    @Transactional(readOnly=true)
    public List<ExamTypeResponse> listTypes(){
        return types.findAll().stream().map(t -> new ExamTypeResponse(t.getCode(), t.getName())).toList();
    }
}
