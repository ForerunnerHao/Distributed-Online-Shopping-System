package com.email.EmailService.messaging;

import com.email.EmailService.dto.EmailEventDTO;
import com.email.EmailService.service.EmailEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener for email events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {
    
    private final EmailEventHandler emailEventHandler;
    private final ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${messaging.queue.email}")
    public void handleEmailEvent(String message) {
        try {
            log.info("Received email event from RabbitMQ: {}", message);
            
            // Parse message to DTO
            EmailEventDTO event = objectMapper.readValue(message, EmailEventDTO.class);
            
            // Handle event
            emailEventHandler.handleEvent(event);
            
        } catch (Exception e) {
            log.error("Error processing email event from RabbitMQ: {}", message, e);
            // Could implement retry logic here or dead letter queue
        }
    }
}

