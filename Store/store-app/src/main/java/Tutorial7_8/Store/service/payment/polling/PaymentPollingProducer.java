package Tutorial7_8.Store.service.payment.polling;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static Tutorial7_8.Store.service.payment.polling.PaymentPollingAmqpConfig.*;

@Service
@RequiredArgsConstructor
public class PaymentPollingProducer {

    private final RabbitTemplate rabbit;

    // first item call
    public void triggerFirstCheck(Long orderId, String idempotencyKey) {
        rabbit.convertAndSend(EX, RK_R1, new PaymentStatusCheckMsg(orderId, idempotencyKey, 0));
    }

    // retry next
    public void resendWithBackoff(PaymentStatusCheckMsg msg) {
        int next = msg.getAttempt() + 1;
        PaymentStatusCheckMsg nextMsg = new PaymentStatusCheckMsg(msg.getOrderId(), msg.getIdempotencyKey(), next);
        switch (next) {
            case 1 -> rabbit.convertAndSend(EX, RK_R2, nextMsg); // 6s
            case 2 -> rabbit.convertAndSend(EX, RK_R3, nextMsg); // 9s
            default -> { /* this case will be handled by consumer */ }
        }
    }

    // optional: send the msg to fail queue for audit
    public void sendFail(PaymentStatusCheckMsg msg) {
        rabbit.convertAndSend(EX, RK_FAIL, msg);
    }
}
