package com.pppk.exams.exams_infra.repos;

import com.pppk.exams.exams_infra.entities.ExamTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public interface ExamTypeRepo extends JpaRepository<ExamTypeEntity, String> {}
