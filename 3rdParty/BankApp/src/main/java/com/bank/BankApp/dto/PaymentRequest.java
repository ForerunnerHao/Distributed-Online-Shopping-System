package com.bank.BankApp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequest {

    @NotBlank
    @Size(max = 32)
    private String sourceAccount;

    @NotBlank
    @Size(max = 32)
    private String destinationAccount;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 64)
    private String orderId;

    @Size(max = 512)
    private String description;

    @Size(max = 512)
    private String callbackUrl;

    @Size(max = 128)
    private String idempotencyKey;

    private boolean simulateFailure;
}
