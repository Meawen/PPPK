package com.pppk.exams.exams_infra.repos;

import com.pppk.exams.exams_infra.entities.ExamAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ExamAttachmentRepo extends JpaRepository<ExamAttachmentEntity, Long> {
    List<ExamAttachmentEntity> findByExamId(Long examId);
}