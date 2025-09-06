package com.pppk.patients.patients_infra.Entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor

@Table(name = "prescriptions")
public class PrescriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @CreationTimestamp
    @Column(name = "prescribed_at", nullable = false, updatable = false)
    private OffsetDateTime prescribedAt;

    @NotNull
    @Column(name = "medication", nullable = false, length = Integer.MAX_VALUE)
    private String medication;

    @Column(name = "dosage", length = Integer.MAX_VALUE)
    private String dosage;

    @Column(name = "instructions", length = Integer.MAX_VALUE)
    private String instructions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}