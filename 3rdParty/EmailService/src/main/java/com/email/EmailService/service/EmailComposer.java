package com.email.EmailService.service;

import com.email.EmailService.domain.EmailTemplate;
import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.dto.EmailEventDTO;
import com.email.EmailService.repository.EmailTemplateRepository;
import com.email.EmailService.util.TemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service to compose email content from templates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailComposer {
    
    private final EmailTemplateRepository templateRepository;
    private final TemplateRenderer templateRenderer;
    
    /**
     * Compose email subject and body based on event type
     */
    public EmailContent compose(EmailEventDTO event) {
        try {
            Optional<EmailTemplate> templateOpt = templateRepository.findByCodeAndEnabledTrue(event.getEventType().name());
            
            if (templateOpt.isEmpty()) {
                throw new RuntimeException("Template not found for event type: " + event.getEventType());
            }
            
            EmailTemplate template = templateOpt.get();
            
            // Build variables map
            Map<String, Object> variables = buildVariables(event);
            
            // Render subject and body
            String subject = templateRenderer.render(template.getSubjectTpl(), variables);
            String body = templateRenderer.render(template.getBodyTpl(), variables);
            
            return EmailContent.builder()
                    .subject(subject)
                    .body(body)
                    .eventType(event.getEventType())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error composing email for event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to compose email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build variables map from event DTO
     */
    private Map<String, Object> buildVariables(EmailEventDTO event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", event.getOrderId());
        variables.put("customerName", event.getCustomerName() != null ? event.getCustomerName() : "Customer");
        variables.put("eventType", event.getEventType().name());
        
        if (event.getTrackingNo() != null) {
            variables.put("trackingNo", event.getTrackingNo());
        }
        
        if (event.getWarehouse() != null) {
            variables.put("warehouse", event.getWarehouse());
        }
        
        // Add extra fields
        if (event.getExtra() != null) {
            variables.putAll(event.getExtra());
        }
        
        return variables;
    }
    
    @lombok.Value
    @lombok.Builder
    public static class EmailContent {
        String subject;
        String body;
        EventType eventType;
    }
}

