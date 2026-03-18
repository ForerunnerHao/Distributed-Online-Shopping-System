package com.bank.BankApp.service;

import com.bank.BankApp.dto.PaymentCallbackPayload;
import com.bank.BankApp.model.BankTransaction;
import com.bank.BankApp.model.enums.CallbackStatus;
import com.bank.BankApp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestClientException;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackDispatcher {

    private final BankTransactionRepository bankTransactionRepository;
    // 发送 HTTP 请求（回调到外部）
    private final RestTemplate restTemplate;

    @Async
    // 监听 PaymentService 中的支付状态变更，事务成功提交之后触发
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentStatusChanged(PaymentService.PaymentStatusChangedEvent event) {
        log.info("enter onPaymentStatusChanged: {}", event.transactionId());
        // 从数据库查交易
        BankTransaction transaction = bankTransactionRepository.findById(event.transactionId())
                .orElse(null);

        // 如果查不到（比如数据已被删），写个警告日志然后退出
        if (transaction == null) {
            log.warn("Received callback event for non-existing transaction {}", event.transactionId());
            return;
        }

        // 如果 callbackUrl 为空，就直接返回。
        // 说明这笔交易没有设置回调地址，不需要通知外部。
        if (!StringUtils.hasText(transaction.getCallbackUrl())) {
            log.info("Try to callback, but this transaction not set callback address {}", event.transactionId());
            return;
        }

        // 每次回调都会 +1 计数，并更新最后一次回调时间
        transaction.setCallbackAttempts(transaction.getCallbackAttempts() + 1);
        transaction.setLastCallbackAt(Instant.now());

        try {
            // 构建一个 JSON 消息，发给 Store
            PaymentCallbackPayload payload = PaymentCallbackPayload.builder()
                    .transactionRef(transaction.getTransactionRef())
                    .orderId(transaction.getOrderId())
                    .type(transaction.getType())
                    .status(transaction.getStatus())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .sourceAccount(transaction.getSourceAccount().getAccountNumber())
                    .destinationAccount(transaction.getDestinationAccount().getAccountNumber())
                    .failureReason(transaction.getFailureReason())
                    .processedAt(transaction.getProcessedAt())
                    .occurredAt(Instant.now())
                    .build();

            // 发送 HTTP 回调
            HttpHeaders headers = new HttpHeaders();
            // 设置 HTTP 头为 JSON
            headers.setContentType(MediaType.APPLICATION_JSON);

//            // 向交易里的 callbackUrl 发起 POST
//            restTemplate.postForEntity(transaction.getCallbackUrl(), new HttpEntity<>(payload, headers), Void.class);
//            // 成功后把 callbackStatus 标记为 DELIVERED 并写日志
//            transaction.setCallbackStatus(CallbackStatus.DELIVERED);
//            log.info("Delivered callback for transaction {} to {}", transaction.getTransactionRef(), transaction.getCallbackUrl());
//        } catch (Exception ex) {
            ResponseEntity<Void> response = restTemplate.exchange(
                    transaction.getCallbackUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                transaction.setCallbackStatus(CallbackStatus.DELIVERED);
                log.info("Delivered callback for transaction {} to {} with status {}",
                        transaction.getTransactionRef(), transaction.getCallbackUrl(), response.getStatusCode());
            } else {
                transaction.setCallbackStatus(CallbackStatus.FAILED);
                log.warn("Callback for transaction {} returned non-success status {}",
                        transaction.getTransactionRef(), response.getStatusCode());
            }
        } catch (RestClientException ex) {
            // 如果 HTTP 请求失败或抛异常，把 callbackStatus 标记为 FAILED，日志记录错误
            transaction.setCallbackStatus(CallbackStatus.FAILED);
            log.error("Failed to deliver callback for [{}] {}: {}", transaction.getCallbackUrl(),transaction.getTransactionRef(), ex.getMessage());
        }

        bankTransactionRepository.save(transaction);
    }
}