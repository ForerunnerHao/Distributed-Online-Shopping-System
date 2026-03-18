package com.bank.BankApp.repository;

import com.bank.BankApp.model.BankTransaction;
import com.bank.BankApp.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    // Optional：一个值可能存在，也可能不存在
    // 此处：可能找到对应的交易，也可能没有
    Optional<BankTransaction> findByTransactionRef(String transactionRef);

    Optional<BankTransaction> findByIdempotencyKey(String idempotencyKey);

    List<BankTransaction> findByOriginalTransactionIdAndType(Long originalTransactionId, TransactionType type);
}