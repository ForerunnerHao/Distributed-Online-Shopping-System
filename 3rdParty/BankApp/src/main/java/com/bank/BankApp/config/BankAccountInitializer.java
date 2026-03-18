package com.bank.BankApp.config;

import com.bank.BankApp.model.BankAccount;
import com.bank.BankApp.model.enums.AccountStatus;
import com.bank.BankApp.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BankAccountInitializer {

    // 注入银行账户仓库接口
    private final BankAccountRepository bankAccountRepository;

    @Bean
    public CommandLineRunner seedAccounts() {
        return args -> {
            List<BankAccount> predefined = List.of(
                    createAccount("STORE-0001", "Store Settlement Account", "store@example.com", new BigDecimal("100000.00")),
                    createAccount("CUSTOMER-0001", "Demo Customer", "customer@example.com", new BigDecimal("5000.00"))
            );

            for (BankAccount account : predefined) {
                bankAccountRepository.findByAccountNumber(account.getAccountNumber())
                        .ifPresentOrElse(existing -> {
                            if (existing.getStatus() != AccountStatus.ACTIVE) {
                                existing.setStatus(AccountStatus.ACTIVE);
                                bankAccountRepository.save(existing);
                            }
                        }, () -> {
                            bankAccountRepository.save(account);
                            log.info("Seeded bank account {}", account.getAccountNumber());
                        });
            }
        };
    }

    private BankAccount createAccount(String accountNumber, String owner, String email, BigDecimal balance) {
        return BankAccount.builder()
                .accountNumber(accountNumber)
                .ownerName(owner)
                .email(email)
                .currency("AUD")
                .balance(balance)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}