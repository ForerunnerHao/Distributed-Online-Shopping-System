package com.bank.BankApp.model;

import com.bank.BankApp.model.enums.CallbackStatus;
import com.bank.BankApp.model.enums.TransactionStatus;
import com.bank.BankApp.model.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bank_transactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_ref", columnNames = "transaction_ref"),
                @UniqueConstraint(name = "uk_transaction_idempotency", columnNames = "idempotency_key")
        })
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 生成的唯一交易流水号 UUID
    @Column(name = "transaction_ref", nullable = false, updatable = false, length = 64)
    private String transactionRef;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    // PENDING / REFUND
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    // PENDING, SUCCEEDED, FAILED, REFUNDED, CANCELLED
    private TransactionStatus status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", nullable = false)
    private BankAccount sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private BankAccount destinationAccount;

    @Column(name = "description", length = 512)
    private String description;

    // 调用方传入的一个唯一键(比如基于 orderId 拼出来)
    // 重复的同笔交易会返回原结果，不扣款
    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    // 由调用方在发起支付时传入，如果传"callback"就是 /api/v1/payments/callback
    @Column(name = "callback_url", length = 512)
    private String callbackUrl;

    // 默认存的是枚举的序号，这里改成存 String
    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status", nullable = false, length = 24)
    private CallbackStatus callbackStatus;

    @Column(name = "callback_attempts", nullable = false)
    private int callbackAttempts;

    @Column(name = "last_callback_at")
    private Instant lastCallbackAt;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "processed_at")
    private Instant processedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // 多条记录可以对应同一条 BankTransaction。一个交易可能有多条退款记录，每条退款记录都指向同一个“原始交易”。
    @ManyToOne(fetch = FetchType.LAZY)
    // 存 BankTransaction 的主键(基于谁的退款)
    @JoinColumn(name = "original_transaction_id")
    private BankTransaction originalTransaction;
}