package com.email.EmailService.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String code;                 // business error code, like VALIDATION_ERROR
    private String message;              // error message
    private String path;                 // request path
    private Instant timestamp;           // timestamp
    private String traceId;              // MDC
    private Map<String, Object> data;    // append info
}