package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** An uploaded PDF resume and the text PDFBox extracted from it. */
@Entity
@Table(name = "resume_upload")
public class ResumeUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath;

    @Column(name = "extracted_text", columnDefinition = "MEDIUMTEXT")
    private String extractedText;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    protected ResumeUpload() {
    }

    public ResumeUpload(Long applicantId, String fileName, String filePath, String extractedText) {
        this.applicantId = applicantId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.extractedText = extractedText;
    }

    public Long getId() { return id; }
    public Long getApplicantId() { return applicantId; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public String getExtractedText() { return extractedText; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
