package com.bank.BankApp.service;

import com.bank.BankApp.dto.PaymentRequest;
import com.bank.BankApp.dto.PaymentResponse;
import com.bank.BankApp.dto.RefundRequest;
import com.bank.BankApp.errors.BusinessException;
import com.bank.BankApp.model.BankAccount;
import com.bank.BankApp.model.BankTransaction;
import com.bank.BankApp.model.enums.AccountStatus;
import com.bank.BankApp.model.enums.CallbackStatus;
import com.bank.BankApp.model.enums.TransactionStatus;
import com.bank.BankApp.model.enums.TransactionType;
import com.bank.BankApp.repository.BankAccountRepository;
import com.bank.BankApp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentResponse initiatePayment(PaymentRequest request) {
        // 根据唯一键Idempotency_key查看是否已存在交易
        BankTransaction existing = resolveIdempotentTransaction(request.getIdempotencyKey());
        if (existing != null) {
            log.info("Returning existing transaction for idempotency key {}", request.getIdempotencyKey());
            return toResponse(existing);
        }

        // 检查双方货币是否相同
        BankAccount source = requireActiveAccount(request.getSourceAccount());
        BankAccount destination = requireActiveAccount(request.getDestinationAccount());
        validateCurrency(source, destination, request.getCurrency());

        BankTransaction transaction = BankTransaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .orderId(request.getOrderId())
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .sourceAccount(source)
                .destinationAccount(destination)
                .description(request.getDescription())
                .idempotencyKey(request.getIdempotencyKey())
                .callbackUrl(normalizeCallback(request.getCallbackUrl()))
                .callbackStatus(StringUtils.hasText(request.getCallbackUrl()) ? CallbackStatus.PENDING : CallbackStatus.DELIVERED)
                .build();

        // 失败比如余额不足的交易同样保存
        bankTransactionRepository.save(transaction);

        if (request.isSimulateFailure()) {
            log.warn("Simulate failure");
            return failTransaction(transaction, "SIMULATED_FAILURE", "Payment simulation requested", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // 判断余额是否足够
        if (source.getBalance().compareTo(request.getAmount()) < 0) {
            return failTransaction(transaction, "INSUFFICIENT_FUNDS", "Insufficient funds", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        try {
            // 付款方扣款
            debit(source, request.getAmount());
            // 收款方收款
            credit(destination, request.getAmount());
        } catch (OptimisticLockingFailureException ex) {
            // 基于BankAccount中的version字段，以乐观锁处理并发冲突
            return failTransaction(transaction, "CONCURRENT_MODIFICATION", "Concurrent balance update detected", HttpStatus.CONFLICT);
        }

        transaction.setStatus(TransactionStatus.SUCCEEDED);
        transaction.setProcessedAt(Instant.now());
        bankTransactionRepository.save(transaction);

        publishStatusChange(transaction);
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String transactionRef) {
        BankTransaction transaction = bankTransactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND));
        return toResponse(transaction);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("IDEMPOTENCY_KEY_REQUIRED", "Idempotency key is required", HttpStatus.BAD_REQUEST);
        }

        BankTransaction transaction = bankTransactionRepository.findByIdempotencyKey(idempotencyKey.trim())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND));
        return toResponse(transaction);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PaymentResponse refund(String transactionRef, RefundRequest request) {
        BankTransaction original = bankTransactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Original payment not found", HttpStatus.NOT_FOUND));

        if (original.getStatus() != TransactionStatus.SUCCEEDED) {
            throw new BusinessException("PAYMENT_NOT_SETTLED", "Only settled payments can be refunded", HttpStatus.BAD_REQUEST);
        }

        if (bankTransactionRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            throw new BusinessException("REFUND_EXISTS", "Refund already processed for this idempotency key", HttpStatus.CONFLICT);
        }

        BankTransaction refund = BankTransaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .orderId(original.getOrderId())
                .type(TransactionType.REFUND)
                .status(TransactionStatus.PENDING)
                .amount(original.getAmount())
                .currency(original.getCurrency())
                .sourceAccount(original.getDestinationAccount())
                .destinationAccount(original.getSourceAccount())
                .description(request.getReason())
                .idempotencyKey(request.getIdempotencyKey())
                .callbackUrl(normalizeCallback(Optional.ofNullable(request.getCallbackUrl()).orElse(original.getCallbackUrl())))
                .callbackStatus(StringUtils.hasText(request.getCallbackUrl()) || StringUtils.hasText(original.getCallbackUrl()) ? CallbackStatus.PENDING : CallbackStatus.DELIVERED)
                .originalTransaction(original)
                .build();

        bankTransactionRepository.save(refund);

        BankAccount debitAccount = original.getDestinationAccount();
        BankAccount creditAccount = original.getSourceAccount();

        if (debitAccount.getBalance().compareTo(refund.getAmount()) < 0) {
            return failTransaction(refund, "INSUFFICIENT_FUNDS", "Store balance not enough for refund", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        try {
            debit(debitAccount, refund.getAmount());
            credit(creditAccount, refund.getAmount());
        } catch (OptimisticLockingFailureException ex) {
            return failTransaction(refund, "CONCURRENT_MODIFICATION", "Concurrent balance update detected", HttpStatus.CONFLICT);
        }

        refund.setStatus(TransactionStatus.SUCCEEDED);
        refund.setProcessedAt(Instant.now());
        bankTransactionRepository.save(refund);

        original.setStatus(TransactionStatus.REFUNDED);
        original.setProcessedAt(Instant.now());
        bankTransactionRepository.save(original);

        publishStatusChange(refund);
        return toResponse(refund);
    }

    private BankTransaction resolveIdempotentTransaction(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        return bankTransactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    private BankAccount requireActiveAccount(String accountNumber) {
        BankAccount account = bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new BusinessException("ACCOUNT_NOT_FOUND", "Account not found", HttpStatus.NOT_FOUND));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "Account is not active", HttpStatus.BAD_REQUEST);
        }
        return account;
    }

    private void validateCurrency(BankAccount source, BankAccount destination, String requestCurrency) {
        String currency = requestCurrency.toUpperCase();
        if (!source.getCurrency().equalsIgnoreCase(currency) || !destination.getCurrency().equalsIgnoreCase(currency)) {
            throw new BusinessException("CURRENCY_MISMATCH", "Currency mismatch", HttpStatus.BAD_REQUEST);
        }
    }

    // 扣款
    private void debit(BankAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);
    }

    // 存储
    private void credit(BankAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);
    }

    // 失败交易的 PaymentResponse
    private PaymentResponse failTransaction(BankTransaction transaction, String errorCode, String message, HttpStatus status) {
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(message);
        transaction.setProcessedAt(Instant.now());
        bankTransactionRepository.save(transaction);
        log.warn("the transaction is fail, try to callback to store");
        publishStatusChange(transaction);
        throw new BusinessException(errorCode, message, status, Map.of("transactionRef", transaction.getTransactionRef()));
    }

    // 发布一个支付状态变更事件，通知其他系统组件
    // 被 PaymentCallbackDispatcher 监听
    private void publishStatusChange(BankTransaction transaction) {
        eventPublisher.publishEvent(new PaymentStatusChangedEvent(transaction.getId()));
    }

    // 处理下 callbackUrl
    private String normalizeCallback(String callbackUrl) {
        // 如果 callbackUrl 是 null、空字符串 ""，或者只包含空格，就返回 null
        if (!StringUtils.hasText(callbackUrl)) {
            return null;
        }
        return callbackUrl.trim();
    }

    private PaymentResponse toResponse(BankTransaction transaction) {
        return PaymentResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .orderId(transaction.getOrderId())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .sourceAccount(transaction.getSourceAccount().getAccountNumber())
                .destinationAccount(transaction.getDestinationAccount().getAccountNumber())
                .description(transaction.getDescription())
                .failureReason(transaction.getFailureReason())
                .callbackUrl(transaction.getCallbackUrl())
                .callbackStatus(transaction.getCallbackStatus())
                .callbackAttempts(transaction.getCallbackAttempts())
                .processedAt(transaction.getProcessedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    // 状态变更事件
    public record PaymentStatusChangedEvent(Long transactionId) {
    }
}