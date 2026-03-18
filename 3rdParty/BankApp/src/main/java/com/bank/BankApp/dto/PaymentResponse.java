package com.bank.BankApp.dto;

import com.bank.BankApp.model.enums.CallbackStatus;
import com.bank.BankApp.model.enums.TransactionStatus;
import com.bank.BankApp.model.enums.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentResponse {
    String transactionRef;
    String orderId;
    // PAYMENT / REFUND
    TransactionType type;
    TransactionStatus status;
    BigDecimal amount;
    String currency;
    String sourceAccount;
    String destinationAccount;
    String description;
    String failureReason;
    String callbackUrl;
    CallbackStatus callbackStatus;
    int callbackAttempts;
    Instant processedAt;
    Instant updatedAt;
}