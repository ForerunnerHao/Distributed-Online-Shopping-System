package com.bank.BankApp.service;

import com.bank.BankApp.dto.AccountResponse;
import com.bank.BankApp.dto.CreateAccountRequest;
import com.bank.BankApp.errors.BusinessException;
import com.bank.BankApp.model.BankAccount;
import com.bank.BankApp.model.enums.AccountStatus;
import com.bank.BankApp.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final BankAccountRepository bankAccountRepository;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        // 根据 AccountNumber 查看是否已存在帐户
        bankAccountRepository.findByAccountNumber(request.getAccountNumber())
                .ifPresent(acc -> {
                    throw new BusinessException("ACCOUNT_EXISTS", "Account number already exists", HttpStatus.CONFLICT);
                });

        BankAccount account = BankAccount.builder()
                .accountNumber(request.getAccountNumber())
                .ownerName(request.getOwnerName())
                .email(request.getEmail())
                .currency(request.getCurrency().toUpperCase())
                .balance(request.getInitialBalance())
                .status(AccountStatus.ACTIVE)
                .build();

        bankAccountRepository.save(account);
        log.info("Created bank account {} for {}", account.getAccountNumber(), account.getOwnerName());
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found", HttpStatus.NOT_FOUND));
        return toResponse(account);
    }

    @Transactional
    public void increaseBalance(BankAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);
    }

    @Transactional
    public void decreaseBalance(BankAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);
    }

    // to DTO format
    private AccountResponse toResponse(BankAccount account) {
        return AccountResponse.builder()
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .email(account.getEmail())
                .status(account.getStatus())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}