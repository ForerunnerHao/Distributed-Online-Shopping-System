package com.bank.BankApp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequest {

    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    @Size(max = 512)
    private String callbackUrl;

    @Size(max = 256)
    private String reason;
}