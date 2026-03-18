package com.bank.BankApp.dto;

import com.bank.BankApp.model.enums.TransactionStatus;
import com.bank.BankApp.model.enums.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentCallbackPayload {
    String transactionRef;
    String orderId;
    TransactionType type;
    TransactionStatus status;
    BigDecimal amount;
    String currency;
    String sourceAccount;
    String destinationAccount;
    String failureReason;
    Instant processedAt;
    Instant occurredAt;
}