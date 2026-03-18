package com.email.EmailService.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    
    @Column(name = "subject_tpl", columnDefinition = "TEXT")
    private String subjectTpl;
    
    @Column(name = "body_tpl", columnDefinition = "TEXT")
    private String bodyTpl;
    
    private Integer version;
    
    private Boolean enabled;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

