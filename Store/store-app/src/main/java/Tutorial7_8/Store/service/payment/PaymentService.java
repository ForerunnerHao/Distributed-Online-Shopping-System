package Tutorial7_8.Store.service.payment;

import Tutorial7_8.Common.enums.*;
import Tutorial7_8.Store.dto.delivery.DeliveryCoRequest;
import Tutorial7_8.Store.dto.payment.*;
import Tutorial7_8.Store.dto.payment.bank.BankCallbackRequest;
import Tutorial7_8.Store.dto.payment.bank.BankRefundRequest;
import Tutorial7_8.Store.dto.payment.bank.BankRequest;
import Tutorial7_8.Store.dto.payment.bank.BankResponse;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.model.Payment;
import Tutorial7_8.Store.model.User;
import Tutorial7_8.Store.repository.OrderRepository;
import Tutorial7_8.Store.repository.PaymentRepository;
import Tutorial7_8.Store.repository.UserRepository;
import Tutorial7_8.Store.service.Email.delaySend.EmailSendProducer;
import Tutorial7_8.Store.service.InventoryService;
import Tutorial7_8.Store.service.delivery.DeliveryService;
import Tutorial7_8.Store.service.payment.polling.PaymentPollingProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;

    private final EmailSendProducer emailSendProducer;

    private final ObjectMapper mapper;
    private static final String BANK_API = "http://localhost:8086/api/v1/payments";
    private static final String BANK_REFUND_API = "http://localhost:8086/api/v1/payments/%s/refund";
    private static final long BANK_TIMEOUT_MS = 2000;

    private final PaymentPollingProducer pollingProducer;

    @Transactional
    public PaymentDTO payOrder(PaymentRequest request, String userId) {
        Long orderId = request.getOrderId();
        String sourceAccount = request.getCustomerAccount();

        // check the order
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Can not find order by provide id", HttpStatus.NOT_FOUND));

        // check the user
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Can not find user by provide id", HttpStatus.NOT_FOUND));

        BigDecimal amount = order.getItem().getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));

        // check the amount
        if (amount.signum() <= 0) {
            throw new BusinessException("BAD_AMOUNT", "Amount must be positive", HttpStatus.BAD_REQUEST);
        }

        // order's status
        OrderStatus orderStatus = order.getStatus();
        orderStatusCheck(orderStatus);


        String idempotencyKey = orderId + ":" + order.getItem().getSku() + ":" + order.getQuantity();

        Payment payment = paymentRepository.findPaymentByIdempotencyKey(idempotencyKey);

        if (payment == null) {
            Payment newPayment = Payment.builder()
                    .sourceAccount(sourceAccount)
                    .idempotencyKey(idempotencyKey)
                    .amount(amount)
                    .order(order)
                    .user(user)
                    .build();
            paymentRepository.saveAndFlush(newPayment);
            payment = newPayment;
        }

        switch (payment.getStatus()) {
            case PAID -> {
                log.warn("Payment already paid");
                throw new BusinessException(
                        "PAYMENT_ALREADY_PAID",
                        "Payment already paid, the transactionRef: " + payment.getTransactionRef(),
                        HttpStatus.BAD_REQUEST
                );
            }
            case REFUNDED, REFUNDED_PENDING -> {
                log.warn("Payment already refund");
                throw new BusinessException(
                        "PAYMENT_ALREADY_REFUND",
                        "Payment already refund, the refundTransactionRef: " + payment.getRefundTransactionRef(),
                        HttpStatus.BAD_REQUEST
                );
            }
            case CANCELED_BY_USER -> {
                log.warn("Payment already canceled by user");
                throw new BusinessException(
                        "PAYMENT_ALREADY_CANCELED_BY_USER",
                        "Payment already canceled by user, the cancel date: " + order.getCanceledAt(),
                        HttpStatus.BAD_REQUEST
                );
            }
            default -> {
            }
        }

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.saveAndFlush(order);

        BankRequest bankRequest = BankRequest.builder()
                .amount(amount)
                .sourceAccount(sourceAccount)
                .orderId(orderId.toString())
                .idempotencyKey(idempotencyKey)
                .simulateFailure(request.isSimulateFailure())
                .description(String.format("User buy item %s x%d, amount: %s",
                        order.getItem().getName(), order.getQuantity(), amount))
                .build();
        log.info("Created a bankRequest, the callback URL si {}", bankRequest.getCallbackUrl());
        // TODO, send request to bank
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(BANK_TIMEOUT_MS))
                .build();

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(bankRequest);
        } catch (JsonProcessingException e) {
            throw new BusinessException("SERIALIZE_ERR", "Serialize bank request failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        log.info("Send payment request to bank, orderId {}", orderId);
        HttpRequest bankHttpReq = HttpRequest.newBuilder()
                .uri(URI.create(BANK_API))
                .timeout(Duration.ofMillis(BANK_TIMEOUT_MS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        BankResponse bankResp;

        try {
            HttpResponse<String> httpResp = client.send(bankHttpReq, HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() / 100 != 2) {
                // if response code not 2xx, as fail
                log.warn("Bank response non-2xx: status={}, body={}", httpResp.statusCode(), httpResp.body());
            }
            bankResp = mapper.readValue(httpResp.body(), BankResponse.class);

            // TODO according to the response status to set the order's status

        } catch (IOException | InterruptedException e) {
            // timeout or network error can not assume the payment fail, need to send request to bank check payment status active
            log.warn("Bank call error: {}", e.toString());
            payment.setStatus(PaymentStatus.PAYMENT_PENDING);
            paymentRepository.save(payment);
            // TODO polling to check the payment status
            log.info("[Start polling], bank response exception: orderId {}", orderId);
            pollingProducer.triggerFirstCheck(payment.getOrder().getId(), payment.getIdempotencyKey());

            return PaymentDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(user.getId())
                    .transactionRef("None")
                    .message("Bank is processing, please refresh later")
                    .build();
        }

        // TODO build the return object
        if (bankResp.getStatus() == TransactionStatus.SUCCEEDED) {
            log.info("Bank payment successful, try to confirm the stock on the warehouse");
            boolean ok = inventoryService.confirmItemForOrder(order.getId());
            if (!ok) {
                // edge case, payment successful, but stock change failed, need manual intervention
                payment.setStatus(PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                throw new BusinessException("WAREHOUSE_CONFIRM_FAIL",
                        "Payment captured but warehouse confirm failed; manual intervention needed",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionRef(bankResp.getTransactionRef());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            try {
                if (bankResp.getType() == TransactionType.PAYMENT) {
                    log.info("Payment successfully, the type is PAYMENT, try to deliver the package.");
                    List<Order.WarehouseReservation> wrs = order.getWarehouseReservations();
                    if (order.getStatus() != OrderStatus.PAID){
                        throw new BusinessException("ORDER_STATUS_ERROR", "Order is not Paid status, can not deliver", HttpStatus.BAD_REQUEST);
                    }

                    log.info("[PaymentService] payOrder - deliverItem");
                    deliveryService.deliverItem(wrs, orderId);
                }
            }
            catch (Exception e)
            {
                log.error("Error while sending deliveries", e);
            }

            return PaymentDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(user.getId())
                    .paymentDate(Instant.now())
                    .paymentAmount(bankResp.getAmount())
                    .orderCreateDate(order.getCreatedAt())
                    .orderExpiredDate(order.getExpiresAt())
                    .paymentType(bankResp.getType().toString())
                    .paymentStatus(bankResp.getStatus().toString())
                    .transactionRef(bankResp.getTransactionRef())
                    .message("Payment captured")
                    .build();
        } else if (bankResp.getStatus() == TransactionStatus.FAILED) {
            log.warn("bank response is failed, enter handle fail process, orderId {}", orderId);
            return handleBankFailed(order, payment, user, bankResp.getTransactionRef());
        } else {
            log.info("Bank returns {}, switch to polling", bankResp.getStatus());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.saveAndFlush(order);

            payment.setStatus(PaymentStatus.PAYMENT_PENDING);
            paymentRepository.saveAndFlush(payment);
            log.info("[Start polling], bank response_status not success or fail: orderId {}", orderId);
            pollingProducer.triggerFirstCheck(payment.getOrder().getId(), payment.getIdempotencyKey());   // 第一次查询排队（1s 后）
            return PaymentDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(user.getId())
                    .transactionRef("None")
                    .message("Bank is processing, please refresh later")
                    .build();
        }
    }

    @Transactional
    protected PaymentDTO handleBankFailed(Order order, Payment payment, User user, String transactionRef) {
        try {
            boolean re = inventoryService.releaseItemForOrder(order.getId());
            if (!re) {
                log.warn("Release after bank failed: {}", order.getId());
                throw new BusinessException("RELEASE_ERROR", "can not release the warehouse stock", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception ex) {
            log.warn("Release after bank failed raised: {}", ex.toString());
            throw new BusinessException("RELEASE_EXCEPTION", "can not release the warehouse stock", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        payment.setStatus(PaymentStatus.PAYMENT_FAILED);
        payment.setTransactionRef(transactionRef);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order.setPaidAt(Instant.now());
        orderRepository.save(order);

        return PaymentDTO.builder()
                .paymentId(payment.getId())
                .orderId(order.getId())
                .userId(user.getId())
                .paymentDate(Instant.now())
                .paymentType(TransactionType.PAYMENT.toString())
                .paymentStatus(TransactionStatus.FAILED.toString())
                .transactionRef(payment.getTransactionRef())
                .message("Payment failed")
                .build();
    }

    @Transactional
    public void bankCallback(BankCallbackRequest callbackRequest) {
        TransactionStatus transactionStatus = callbackRequest.getStatus();
        String orderId = callbackRequest.getOrderId();

        Order order = orderRepository.findByIdForUpdate(Long.parseLong(orderId))
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Can not find payment by order", HttpStatus.NOT_FOUND));

        if (transactionStatus == TransactionStatus.FAILED) {
            log.warn("Bank callback failed, orderId={}", orderId);
            // release the warehouse stock
            boolean ok = inventoryService.releaseItemForOrder(order.getId());
            if (!ok) {
                log.warn("Bank callback: Release warehouse stock failed: {}", orderId);
            }
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setCanceledAt(Instant.now());
            payment.setStatus(PaymentStatus.PAYMENT_FAILED);
            payment.setTransactionRef(callbackRequest.getTransactionRef());
            orderRepository.save(order);
            paymentRepository.save(payment);

        } else if (transactionStatus == TransactionStatus.SUCCEEDED) {
            log.info("Bank callback successful, orderId={}", orderId);
            boolean ok = inventoryService.confirmItemForOrder(order.getId());
            if (!ok) {
                log.warn("Bank callback: Confirm warehouse stock failed: {}", orderId);
            }
            order.setStatus(OrderStatus.PAID);
            order.setPaidAt(Instant.now());
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionRef(callbackRequest.getTransactionRef());
            orderRepository.save(order);
            paymentRepository.save(payment);

            if (callbackRequest.getType() == TransactionType.PAYMENT) {
                log.info("Call back Payment successfully, the type is PAYMENT, try to deliver the package.");
                if (order.getStatus() != OrderStatus.PAID){
                    return;
                }
                log.info("[PaymentService] bankCallback - deliverItem");
//                deliveryService.deliverItem(order.getWarehouseReservations(), Long.valueOf(orderId));
            }

        } else {
            log.warn("Bank callback happen exception, please check the log, orderId={}", orderId);
        }
    }

    @Transactional
    public PaymentDTO refund(RefundRequest refundRequest, String userId) {
        Long orderId = refundRequest.getOrderId();
        // check the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "Can not find order by provide id", HttpStatus.NOT_FOUND));
        // check the user
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Can not find user by provide id", HttpStatus.NOT_FOUND));

        Payment payment = paymentRepository.findFirstByOrderIdOrderByIdDesc(order.getId())
                .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND", "Can not find payment by order", HttpStatus.NOT_FOUND));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "Payment does not belong to the requesting user.", HttpStatus.FORBIDDEN);
        }

        String transactionRef = payment.getTransactionRef();

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("REFUND_REDUNDANT", "This order has refunded the refund transactionRef: " + payment.getRefundTransactionRef(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (transactionRef == null || payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException("REFUND_ERROR", "Please completed payment first", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Check if order is in shipping status - cannot refund if shipping has started
        OrderStatus orderStatus = order.getStatus();
        if (hasShippingStarted(orderStatus)) {
            throw new BusinessException("REFUND_NOT_ALLOWED", 
                    "Cannot refund order in shipping status: " + orderStatus, HttpStatus.CONFLICT);
        }
        String refundIdempotencyKey = "Refund:" + order.getId().toString() + ":" + order.getItem().getSku() + ":" + order.getQuantity().toString();
        BankRefundRequest bankRefundRequest = BankRefundRequest.builder()
                .reason(refundRequest.getReason())
                .idempotencyKey(refundIdempotencyKey)
                .build();

        // TODO, send request to bank
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(BANK_TIMEOUT_MS))
                .build();

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(bankRefundRequest);
        } catch (JsonProcessingException e) {
            throw new BusinessException("SERIALIZE_ERR", "Serialize bank request failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        // TODO insert transactionRef into BANK_REFUND_API "http://localhost:8086/api/v1/payments/%s/refund"
        String url = String.format(
                BANK_REFUND_API,
                URLEncoder.encode(transactionRef, StandardCharsets.UTF_8) // 防特殊字符
        );
        HttpRequest bankHttpReq = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(BANK_TIMEOUT_MS))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        BankResponse bankResp;

        try {
            HttpResponse<String> httpResp = client.send(bankHttpReq, HttpResponse.BodyHandlers.ofString());
            if (httpResp.statusCode() / 100 != 2) {
                // if response code not 2xx, as fail
                log.warn("Bank refund response non-2xx: status={}, body={}", httpResp.statusCode(), httpResp.body());
            }
            bankResp = mapper.readValue(httpResp.body(), BankResponse.class);

            emailSendProducer.sendEmailByStatus(String.valueOf(orderId), OrderStatus.REFUNDED, "Refund order");
        } catch (IOException | InterruptedException e) {
            // timeout or network error can not assume the payment fail, need to send request to bank check payment status active
            log.warn("Bank refund call error: {}", e.toString());
            payment.setStatus(PaymentStatus.REFUNDED_PENDING);
            paymentRepository.save(payment);
            // TODO polling to check the payment status
            return PaymentDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(user.getId())
                    .transactionRef("None")
                    .message("Bank request error, please try again later")
                    .build();
        }

        // TODO build the return object
        if (bankResp.getStatus() == TransactionStatus.SUCCEEDED) {
            log.info("Bank refund successful, try to release the stock on the warehouse");
            boolean ok = inventoryService.releaseItemForOrder(order.getId());
            if (!ok) {
                // edge case, payment successful, but stock change failed, need manual intervention
                payment.setStatus(PaymentStatus.PROCESSING);
                paymentRepository.save(payment);
                throw new BusinessException("WAREHOUSE_RELEASE_FAIL",
                        "Payment captured but warehouse release failed; manual intervention needed",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundTransactionRef(bankResp.getTransactionRef());
            paymentRepository.save(payment);

            order.setStatus(OrderStatus.CANCELED_BY_USER);
            orderRepository.save(order);

            return PaymentDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(user.getId())
                    .paymentAmount(bankResp.getAmount())
                    .paymentType(bankResp.getType().toString())
                    .paymentStatus(bankResp.getStatus().toString())
                    .transactionRef(payment.getTransactionRef())
                    .refundTransactionRef(bankResp.getTransactionRef())
                    .message("Refund captured")
                    .build();
        } else if (bankResp.getStatus() == TransactionStatus.FAILED) {
            return handleBankFailed(order, payment, user, bankResp.getTransactionRef());
        } else {
            throw new BusinessException("REFUND_ERROR", "Refund exception, no success, no fail, please check the log", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void markSuccessAndProceed(BankResponse bankResponse, Long orderId, String idempotencyKey) {
        Payment payment = paymentRepository.findPaymentByIdempotencyKey(idempotencyKey);
        if (payment == null) {
            throw new BusinessException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND);
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setTransactionRef(bankResponse.getTransactionRef());
        log.info("Retry successfully! order: {}, idempotencyKey: {}, transactionRef {}, amount {}", orderId, idempotencyKey, bankResponse.getTransactionRef(), bankResponse.getAmount());
        paymentRepository.saveAndFlush(payment);

        if (bankResponse.getType() == TransactionType.PAYMENT) {
            log.info("Polling request, Payment successfully, the type is PAYMENT, try to deliver the package.");
            Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            if (order.getStatus() != OrderStatus.PAID){
                return;
            }
            log.info("[PaymentService] markSuccessAndProceed - deliverItem");
            deliveryService.deliverItem(order.getWarehouseReservations(), orderId);

        }
    }

    @Transactional
    public void cancelOrderAndRelease(Long orderId, String pollingExhausted) {
        log.warn("cancel order: {}, pollingExhausted: {}", orderId, pollingExhausted);

        boolean r = inventoryService.releaseItemForOrder(orderId);
        if (!r) {
            throw new BusinessException("ORDER_RELEASE_FAIL", "Order release failed, please manually handle", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(
                () -> new BusinessException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND)
        );
        order.setCanceledAt(Instant.now());
        order.setStatus(OrderStatus.CANCELED_BY_SYSTEM);
    }

    public Payment getPayment(String idempotencyKey) {
        return paymentRepository.findPaymentByIdempotencyKey(idempotencyKey);
    }

    public PaymentDTO getPaymentByPaymentId(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(
                () -> new BusinessException("PAYMENT_NOT_FOUND", "Payment not found", HttpStatus.NOT_FOUND)
        );
        return PaymentDTO.builder()
                .paymentId(payment.getId())
                .paymentAmount(payment.getAmount())
                .paymentDate(payment.getCreatedAt())
                .paymentStatus(payment.getStatus().toString())
                .transactionRef(payment.getTransactionRef())
                .refundTransactionRef(payment.getTransactionRef())
                .userId(payment.getUser().getId())
                .orderId(payment.getOrder().getId())
                .build();
    }

    public List<PaymentDTO> getPaymentsByUserId(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND)
        );

        List<Payment> payments = paymentRepository.findByUser((user));
        List<PaymentDTO> dtos = new ArrayList<>();

        for (Payment payment : payments) {
            dtos.add(
                    PaymentDTO.builder()
                            .paymentId(payment.getId())
                            .paymentAmount(payment.getAmount())
                            .paymentDate(payment.getCreatedAt())
                            .paymentStatus(payment.getStatus().toString())
                            .transactionRef(payment.getTransactionRef())
                            .refundTransactionRef(payment.getTransactionRef())
                            .userId(payment.getUser().getId())
                            .orderId(payment.getOrder().getId())
                            .build()
            );
        }

        return dtos;
    }

    public void orderStatusCheck(OrderStatus orderStatus) {
        if (orderStatus == OrderStatus.CANCELED_BY_USER){
            throw new BusinessException("ORDER_CANCELED_BY_USER", "Can not cancel order by provide id", HttpStatus.BAD_REQUEST);
        }

        if (orderStatus == OrderStatus.PAID){
            throw new BusinessException("ORDER_PAID", "Can not pay order by provide id", HttpStatus.BAD_REQUEST);
        }

        if (orderStatus == OrderStatus.PAYMENT_PENDING){
            throw new BusinessException("ORDER_PAYMENT_PENDING", "Can not pay order by provide id", HttpStatus.BAD_REQUEST);
        }

        if (orderStatus == OrderStatus.REFUNDED){
            throw new BusinessException("ORDER_REFUNDED", "Can not pay order by provide id", HttpStatus.BAD_REQUEST);
        }

        if (orderStatus == OrderStatus.CANCELED_BY_SYSTEM){
            throw new BusinessException("ORDER_CANCELED_BY_SYSTEM", "Can not pay order by provide id", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasShippingStarted(OrderStatus status) {
        return status == OrderStatus.SHIPMENT_ACCEPTED
                || status == OrderStatus.PICKED_UP
                || status == OrderStatus.IN_TRANSIT
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.DELIVERY_LOST
                || status == OrderStatus.SHIPMENT_REQUESTED;
    }
}
