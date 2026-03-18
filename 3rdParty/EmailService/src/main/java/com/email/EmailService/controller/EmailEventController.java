package com.email.EmailService.controller;

import com.email.EmailService.domain.EmailContent;
import com.email.EmailService.domain.EmailLog;
import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.domain.enums.SendStatus;
import com.email.EmailService.dto.EmailEventDTO;
import com.email.EmailService.dto.EmailLogResponse;
import com.email.EmailService.dto.PageResponse;
import com.email.EmailService.repository.EmailLogRepository;
import com.email.EmailService.service.EmailEventHandler;
import com.email.EmailService.service.EmailComposer;
import com.email.EmailService.service.EmailSender;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/emails")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email API", description = "Email event and management API")
public class EmailEventController {
    
    private final EmailEventHandler emailEventHandler;
    private final EmailLogRepository emailLogRepository;
    private final EmailSender emailSender;
    
    /**
     * POST /api/v1/emails/order-events
     * Receive order event and send email
     */
    @PostMapping("/order-events")
    @Operation(summary = "Receive order event", description = "Receive order event and send email")
    public ResponseEntity<String> receiveOrderEvent(@Valid @RequestBody EmailEventDTO event) {
        log.info("Received order event: {}", event.getEventType());
        
        // Asynchronously process event (in real scenario, should use @Async)
        EmailLog emailLog = emailEventHandler.handleEvent(event);
        
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Event accepted, email log ID: " + emailLog.getId());
    }

    @PostMapping
    public ResponseEntity<EmailLog> getStoreRequest( @RequestBody EmailContent content) {
        try {
            EmailLog emailLog = emailSender.send(content.getTo(), content.getOrderId(), content.getEventType(),
                    EmailComposer.EmailContent.builder()
                            .subject(content.getSubject())
                            .body(content.getBody())
                            .eventType(content.getEventType())
                            .build());
            return ResponseEntity.ok(emailLog);
        } catch (Exception e) {
            log.error("Error sending email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/v1/emails/send
     * Manually send email (for testing/debugging)
     */
    @PostMapping("/send")
    @Operation(summary = "Send email manually", description = "Send email manually for testing/debugging")
    public ResponseEntity<EmailLogResponse> sendEmail(
            @RequestParam String toEmail,
            @RequestParam(required = false) String orderId,
            @RequestParam String subject,
            @RequestParam String body,
            @RequestParam(required = false, defaultValue = "DELIVERY_ON_TRUCK") EventType eventType) {
        
        try {
            EmailLog emailLog = emailSender.send(toEmail, orderId, eventType, 
                    EmailComposer.EmailContent.builder()
                            .subject(subject)
                            .body(body)
                            .eventType(eventType)
                            .build());
            
            return ResponseEntity.ok(toEmailLogResponse(emailLog));
        } catch (Exception e) {
            log.error("Error sending email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/v1/emails/{id}
     * Get email log by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get email log by ID", description = "Get email log by ID")
    public ResponseEntity<EmailLogResponse> getEmailLog(@PathVariable UUID id) {
        return emailLogRepository.findById(id)
                .map(emailLog -> ResponseEntity.ok(toEmailLogResponse(emailLog)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * GET /api/v1/emails
     * Query email logs with filters
     */
    @GetMapping
    @Operation(summary = "Query email logs", description = "Query email logs with filters")
    public ResponseEntity<PageResponse<EmailLogResponse>> getEmailLogs(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) SendStatus status,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EmailLog> emailLogPage;
        
        if (orderId != null && eventType != null) {
            emailLogPage = emailLogRepository.findByOrderIdAndEventType(orderId, eventType, pageable);
        } else if (orderId != null && status != null) {
            emailLogPage = emailLogRepository.findByOrderIdAndStatus(orderId, status, pageable);
        } else if (orderId != null) {
            emailLogPage = emailLogRepository.findByOrderId(orderId, pageable);
        } else if (eventType != null) {
            emailLogPage = emailLogRepository.findByEventType(eventType, pageable);
        } else if (status != null) {
            emailLogPage = emailLogRepository.findByStatus(status, pageable);
        } else if (email != null) {
            emailLogPage = emailLogRepository.findByToEmail(email, pageable);
        } else {
            emailLogPage = emailLogRepository.findAll(pageable);
        }
        
        List<EmailLogResponse> content = emailLogPage.getContent().stream()
                .map(this::toEmailLogResponse)
                .collect(Collectors.toList());
        
        PageResponse<EmailLogResponse> response = PageResponse.<EmailLogResponse>builder()
                .content(content)
                .totalElements(emailLogPage.getTotalElements())
                .totalPages(emailLogPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    private EmailLogResponse toEmailLogResponse(EmailLog emailLog) {
        return EmailLogResponse.builder()
                .id(emailLog.getId())
                .orderId(emailLog.getOrderId())
                .toEmail(emailLog.getToEmail())
                .eventType(emailLog.getEventType())
                .subject(emailLog.getSubject())
                .body(emailLog.getBody())
                .status(emailLog.getStatus())
                .providerResponse(emailLog.getProviderResponse())
                .createdAt(emailLog.getCreatedAt())
                .sentAt(emailLog.getSentAt())
                .build();
    }
}

