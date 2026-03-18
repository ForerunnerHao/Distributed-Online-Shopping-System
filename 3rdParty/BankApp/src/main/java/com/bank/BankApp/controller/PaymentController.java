package com.bank.BankApp.controller;

import com.bank.BankApp.dto.PaymentRequest;
import com.bank.BankApp.dto.PaymentResponse;
import com.bank.BankApp.dto.RefundRequest;
import com.bank.BankApp.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 发起支付请求
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("POST /initiatePayment");
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 根据交易参考号获取支付信息
    @GetMapping("/{transactionRef}")
    public PaymentResponse getPayment(@PathVariable String transactionRef) {
        return paymentService.getPayment(transactionRef);
    }

    @GetMapping("/idempotency/{idempotencyKey}")
    public PaymentResponse getPaymentByIdempotencyKey(@PathVariable String idempotencyKey) {
        log.info("GET /idempotency/{idempotencyKey}");
        PaymentResponse response = paymentService.getPaymentByIdempotencyKey(idempotencyKey);
        log.info("GET /idempotency/{idempotencyKey:{}}", idempotencyKey);
        log.info("GET PaymentResponse response:{}", response.toString());
        return response;
    }

    // 处理退款请求
    @PostMapping("/{transactionRef}/refund")
    public PaymentResponse refundPayment(@PathVariable String transactionRef,
                                         @Valid @RequestBody RefundRequest request) {
        return paymentService.refund(transactionRef, request);
    }
}
