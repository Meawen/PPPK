package com.pppk.exams.exams_infra.repos;

import com.pppk.exams.exams_infra.entities.ExamEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public interface ExamRepo extends JpaRepository<ExamEntity, Long> {
    Page<ExamEntity> findByPatientId(Long patientId, Pageable p);

}