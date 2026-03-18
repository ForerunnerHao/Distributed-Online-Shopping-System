package com.bank.BankApp.dto;

import com.bank.BankApp.model.enums.AccountStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class AccountResponse {
    String accountNumber;
    String ownerName;
    String email;
    AccountStatus status;
    BigDecimal balance;
    String currency;
    Instant updatedAt;
}