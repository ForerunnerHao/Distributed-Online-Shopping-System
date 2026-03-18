package com.email.EmailService.repository;

import com.email.EmailService.domain.EmailLog;
import com.email.EmailService.domain.enums.EventType;
import com.email.EmailService.domain.enums.SendStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, UUID> {
    Page<EmailLog> findByOrderId(String orderId, Pageable pageable);
    Page<EmailLog> findByEventType(EventType eventType, Pageable pageable);
    Page<EmailLog> findByStatus(SendStatus status, Pageable pageable);
    Page<EmailLog> findByToEmail(String toEmail, Pageable pageable);
    Page<EmailLog> findByOrderIdAndEventType(String orderId, EventType eventType, Pageable pageable);
    Page<EmailLog> findByOrderIdAndStatus(String orderId, SendStatus status, Pageable pageable);
}

