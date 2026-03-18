package com.email.EmailService.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "recipient")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recipient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(length = 128)
    private String name;
    
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}

