package com.email.EmailService.service;

import com.email.EmailService.domain.EmailLog;
import com.email.EmailService.domain.Recipient;
import com.email.EmailService.dto.EmailEventDTO;
import com.email.EmailService.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central handler for email events (from MQ or REST)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailEventHandler {
    
    private final EmailComposer emailComposer;
    private final EmailSender emailSender;
    private final RecipientRepository recipientRepository;
    
    /**
     * Process email event
     */
    @Transactional
    public EmailLog handleEvent(EmailEventDTO event) {
        log.info("Processing email event: {} for order: {}", event.getEventType(), event.getOrderId());
        
        try {
            // Ensure recipient exists
            ensureRecipientExists(event.getToEmail());
            
            // Compose email content
            EmailComposer.EmailContent content = emailComposer.compose(event);
            
            // Send email
            EmailLog emailLog = emailSender.send(
                    event.getToEmail(),
                    event.getOrderId(),
                    event.getEventType(),
                    content
            );
            
            log.info("Email event processed successfully, emailLogId: {}", emailLog.getId());
            return emailLog;
            
        } catch (Exception e) {
            log.error("Failed to process email event: {}", event.getEventId(), e);
            
            // Save failed email log
            return emailSender.sendFailed(
                    event.getToEmail(),
                    event.getOrderId(),
                    event.getEventType(),
                    e.getMessage()
            );
        }
    }
    
    /**
     * Ensure recipient exists in database
     */
    private void ensureRecipientExists(String email) {
        if (!recipientRepository.findByEmail(email).isPresent()) {
            Recipient recipient = Recipient.builder()
                    .email(email)
                    .build();
            recipientRepository.save(recipient);
            log.debug("Created new recipient: {}", email);
        }
    }
}

