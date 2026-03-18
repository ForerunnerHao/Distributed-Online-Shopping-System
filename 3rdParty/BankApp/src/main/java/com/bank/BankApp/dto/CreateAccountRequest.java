package com.bank.BankApp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateAccountRequest {

    @NotBlank
    @Size(max = 32)
    private String accountNumber;

    @NotBlank
    @Size(max = 128)
    private String ownerName;

    @Email
    @Size(max = 128)
    private String email;

    @NotNull
    @PositiveOrZero
    private BigDecimal initialBalance;

//    @NotBlank
    @Size(min = 3, max = 3)
//    private String currency;
    private String currency = "AUD";  // 默认值
}
