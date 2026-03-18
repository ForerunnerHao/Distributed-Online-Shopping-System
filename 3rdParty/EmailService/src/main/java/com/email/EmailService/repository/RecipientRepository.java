package com.email.EmailService.repository;

import com.email.EmailService.domain.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    Optional<Recipient> findByEmail(String email);
}

