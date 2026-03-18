package com.email.EmailService.domain;

import com.email.EmailService.domain.enums.EventType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailContent {
    String to;
    String subject;
    String body;
    String orderId;
    EventType eventType;
}

