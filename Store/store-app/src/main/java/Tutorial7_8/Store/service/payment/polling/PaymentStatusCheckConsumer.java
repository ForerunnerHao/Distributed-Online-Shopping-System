package Tutorial7_8.Store.service.payment.polling;

import Tutorial7_8.Common.enums.PaymentStatus;
import Tutorial7_8.Common.enums.TransactionStatus;
import Tutorial7_8.Store.dto.payment.bank.BankResponse;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.model.Payment;
import Tutorial7_8.Store.service.InventoryService;
import Tutorial7_8.Store.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static Tutorial7_8.Store.service.payment.polling.PaymentPollingAmqpConfig.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusCheckConsumer {

    private final PaymentService paymentService;
    private final PaymentPollingProducer producer;
    private final InventoryService inventoryService;

    private final ObjectMapper mapper;

//    @Value("${bank.api.timeout-ms:3000}")
    private final int bankTimeoutMs = 3000;

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(bankTimeoutMs))
                .build();
    }

    @RabbitListener(queues = Q_MAIN)
    public void onMessage(PaymentStatusCheckMsg msg) {

        String idempotencyKey = msg.getIdempotencyKey();
        Long orderId = msg.getOrderId();
        int attempt = msg.getAttempt();

        log.info("Retry payment, order: {}, idempotencyKey: {}, attempt: {}", orderId, idempotencyKey, attempt);

        try {
            // get the payment status
            Payment payment = paymentService.getPayment(idempotencyKey);
            if (payment != null) {
                if (payment.getStatus() == PaymentStatus.PAID) {
                    log.info("[stop polling] Payment had paid, not polling request: {}", payment.getStatus());
                    return;
                } else if (payment.getStatus() == PaymentStatus.FAILED) {
                    log.info("[stop polling] Payment had failed, not polling request: {}", payment.getStatus());
                    return;
                } else if (payment.getStatus() == PaymentStatus.PAYMENT_FAILED) {
                    log.info("[stop polling] Payment had payment failed, not polling request: {}", payment.getStatus());
                    return;
                }
            }
            BankResponse resp = queryBankStatus(idempotencyKey);

            TransactionStatus status = resp.getStatus();
            switch (status) {
                case SUCCEEDED -> {
                    log.info("[polling] orderId {} - idempotencyKey {} SUCCEEDED on attempt {}", orderId, idempotencyKey,attempt);
                    // make sure the warehouse stork are confirmed
                    boolean confirmed = inventoryService.confirmItemForOrder(orderId);
                    if (!confirmed) {
                        log.warn("Payment captured but warehouse confirm failed during polling :{} - {} - ", orderId, idempotencyKey);
                        return;
                    }
                    paymentService.markSuccessAndProceed(
                            resp,
                            orderId,
                            idempotencyKey
                    );
                }
                case FAILED -> {
                    log.info("[polling] idempotencyKey {} FAILED by bank on attempt {}, cancel.", idempotencyKey, attempt);
                    inventoryService.releaseItemForOrder(orderId);
                    paymentService.markSuccessAndProceed(
                            resp,
                            orderId,
                            idempotencyKey
                    );
                    producer.sendFail(msg);  // optional
                }

                default -> { // PENDING / PROCESSING / UNKNOWN...
                    if (attempt >= 2) { // try 3 times
                        log.warn("[polling] idempotencyKey {} exhausted after 3 attempts, cancel.", idempotencyKey);
                        try {
                            paymentService.cancelOrderAndRelease(orderId, "Polling exhausted");
                        }catch (Exception e) {
                            log.error("Cancel order failed, orderId: {}, idempotencyKey: {}, attempt: {}", orderId, idempotencyKey, attempt,e);
                        }finally {
                            producer.sendFail(msg);
                        }
                    } else {
                        producer.resendWithBackoff(msg); // enter next level retry
                    }
                }
            }

        } catch (Exception ex) {
            log.warn("[polling] idempotencyKey {} query error on attempt {}: {}", idempotencyKey, attempt, ex.toString());
            if (attempt >= 2) {
                try {
                    paymentService.cancelOrderAndRelease(orderId, "Polling exhausted");
                }catch (Exception e) {
                    log.error("Cancel order failed, orderId: {}, idempotencyKey: {}, attempt: {}", orderId, idempotencyKey, attempt,e);
                }finally {
                    producer.sendFail(msg);
                }
            } else {
                producer.resendWithBackoff(msg);
            }
        }
    }


    private BankResponse queryBankStatus(String idempotencyKey) throws Exception {
        String url = String.format("http://localhost:8086/api/v1/payments/idempotency/%s", idempotencyKey);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(bankTimeoutMs))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            // 非 2xx 当作 pending，让外层逻辑走退避
            throw new RuntimeException("Bank status HTTP non-2xx: " + resp.statusCode() + ", body=" + resp.body());
        }
        return mapper.readValue(resp.body(), BankResponse.class);
    }
}
