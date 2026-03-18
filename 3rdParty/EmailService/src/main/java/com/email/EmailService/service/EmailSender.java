package com.email.EmailService.service;

import com.email.EmailService.domain.EmailLog;
import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.domain.enums.SendStatus;
import com.email.EmailService.repository.EmailLogRepository;
import com.email.EmailService.service.EmailComposer.EmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service to send emails (prints to console as per requirements)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSender {
    
    private final EmailLogRepository emailLogRepository;
    
    /**
     * Send email by printing to console (as per requirements)
     * This simulates email sending without actually sending emails
     */
    @Transactional
    public EmailLog send(String toEmail, String orderId, EventType eventType, 
                          EmailContent content) {
        try {
            // Print email to console
            log.info("=".repeat(80));
            log.info("Email sent to: {}", toEmail);
            log.info("Order ID: {}", orderId);
            log.info("Event Type: {}", eventType);
            log.info("Subject: {}", content.getSubject());
            log.info("Body:\n{}", content.getBody());
            log.info("=".repeat(80));
            
            // Save email log
            EmailLog emailLog = EmailLog.builder()
                    .orderId(orderId)
                    .toEmail(toEmail)
                    .eventType(eventType)
                    .subject(content.getSubject())
                    .body(content.getBody())
                    .status(SendStatus.SENT)
                    .providerResponse("PRINTED")
                    .sentAt(LocalDateTime.now())
                    .build();
            
            return emailLogRepository.save(emailLog);
            
        } catch (Exception e) {
            log.error("Error sending email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send failed email log
     */
    @Transactional
    public EmailLog sendFailed(String toEmail, String orderId, EventType eventType, 
                                 String errorMessage) {
        log.error("Failed to send email to: {} for order: {}, error: {}", 
                 toEmail, orderId, errorMessage);
        
        EmailLog emailLog = EmailLog.builder()
                .orderId(orderId)
                .toEmail(toEmail)
                .eventType(eventType)
                .status(SendStatus.FAILED)
                .providerResponse("Failed: " + errorMessage)
                .build();
        
        return emailLogRepository.save(emailLog);
    }
    
}

