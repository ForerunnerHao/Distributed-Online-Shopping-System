package com.bank.BankApp.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> data;

    public BusinessException(String code, String message, HttpStatus status) {
        this(code, message, status, Map.of());
    }

    public BusinessException(String code, String message, HttpStatus status, Map<String, Object> data) {
        super(message);
        this.code = code;
        this.status = status;
        this.data = data;
    }
}
