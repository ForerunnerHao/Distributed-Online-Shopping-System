package com.email.EmailService.repository;

import com.email.EmailService.domain.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    Optional<EmailTemplate> findByCode(String code);
    Optional<EmailTemplate> findByCodeAndEnabledTrue(String code);
}

