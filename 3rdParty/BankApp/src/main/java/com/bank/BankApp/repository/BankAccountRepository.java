package com.bank.BankApp.repository;

import com.bank.BankApp.model.BankAccount;
import com.bank.BankApp.model.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByAccountNumberAndStatus(String accountNumber, AccountStatus status);
}