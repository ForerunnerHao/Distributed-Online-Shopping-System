package com.email.EmailService.dto;

import com.email.EmailService.domain.enums.EventType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEventDTO {
    @NotBlank(message = "eventId is required")
    private String eventId;
    
    @NotNull(message = "eventType is required")
    private EventType eventType;
    
    @NotNull(message = "occurredAt is required")
    private Instant occurredAt;
    
    @NotBlank(message = "orderId is required")
    private String orderId;
    
    @Email(message = "toEmail must be a valid email address")
    @NotBlank(message = "toEmail is required")
    private String toEmail;
    
    private String customerName;
    private String trackingNo;
    private String warehouse;
    private Map<String, Object> extra;
}

