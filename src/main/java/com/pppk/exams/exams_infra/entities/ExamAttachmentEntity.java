package com.pppk.exams.exams_infra.entities;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;

@Getter @Setter
@Entity @Table(name = "exam_attachments")
public class ExamAttachmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private ExamEntity exam;                   // <-- gives you setExam(...)

    @Column(name="object_key", nullable=false, unique=true) private String objectKey;
    @Column(name="content_type")  private String contentType;
    @Column(name="size_bytes")    private Long sizeBytes;
    @Column(name="sha256_hex")    private String sha256Hex;

    @CreationTimestamp @Column(name="created_at", updatable=false, nullable=false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp   @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
}
