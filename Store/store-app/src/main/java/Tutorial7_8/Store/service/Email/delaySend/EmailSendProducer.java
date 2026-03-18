package Tutorial7_8.Store.service.Email.delaySend;

import Tutorial7_8.Common.enums.EventType;
import Tutorial7_8.Common.enums.OrderStatus;
import Tutorial7_8.Store.dto.email.EmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static Tutorial7_8.Store.service.Email.delaySend.EmailSendAmqpConfig.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendProducer {

    private final RabbitTemplate rabbit;

    public void sendEmail(EmailRequest msg) {
        log.info("[MQ send] Sending email to {}", msg.getTo());
        rabbit.convertAndSend(EX_EMAIL_SEND, RK_ES_SEND, msg);
    }

    // optional: send the msg to fail queue for audit
    public void sendFail(EmailRequest msg) {
        rabbit.convertAndSend(EX_EMAIL_SEND, RK_ES_FAIL, msg);
    }

    public void sendEmailByStatus(String orderId, OrderStatus status, String body) {
        EventType eventType = switch (status) {
            case CANCELLED, CANCELED_BY_USER -> EventType.ORDER_CANCELLED;
            case IN_TRANSIT -> EventType.DELIVERY_ON_TRUCK;
            case DELIVERED -> EventType.DELIVERED;
            case REFUNDED -> EventType.REFUND_COMPLETED;
            case REFUND_PENDING -> EventType.REFUND_INITIATED;
            case PAYMENT_FAILED -> EventType.ORDER_FAILED;
            case PICKED_UP -> EventType.DELIVERY_PICKED_UP;
            default -> EventType.DELIVERY_PICKED_UP;
        };

        EmailRequest er = EmailRequest.builder()
                .to("user's email address")
                .subject(body)
                .body("Email Notify")
                .orderId(orderId)
                .eventType(eventType)
                .build();

        try {
            log.info("[MQ send] Sending email to {}", er.getTo());
            rabbit.convertAndSend(EX_EMAIL_SEND, RK_ES_SEND, er);
        } catch (Exception e) {
            log.warn("[sendEmailByStatus] Can not send email message to the queue");
            log.error(e.getMessage());
        }
    }
}
