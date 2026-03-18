package com.email.EmailService.dto;

import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.domain.enums.SendStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLogResponse {
    private UUID id;
    private String orderId;
    private String toEmail;
    private EventType eventType;
    private String subject;
    private String body;
    private SendStatus status;
    private String providerResponse;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}

