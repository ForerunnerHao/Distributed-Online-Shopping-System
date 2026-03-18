package com.email.EmailService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendEmailRequest {
    @Email(message = "Must be a valid email address")
    @NotBlank(message = "Email is required")
    private String toEmail;
    
    private String toName;
    private String orderId;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    @NotBlank(message = "Body is required")
    private String body;
}

