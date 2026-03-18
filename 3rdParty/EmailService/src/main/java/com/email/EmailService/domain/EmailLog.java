package com.email.EmailService.domain;

import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.domain.enums.SendStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "order_id", length = 64)
    private String orderId;
    
    @Column(name = "to_email", length = 255)
    private String toEmail;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 64)
    private EventType eventType;
    
    @Column(columnDefinition = "TEXT")
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SendStatus status;
    
    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}

