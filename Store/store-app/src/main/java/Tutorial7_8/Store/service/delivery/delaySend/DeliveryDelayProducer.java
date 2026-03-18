package Tutorial7_8.Store.service.delivery.delaySend;

import Tutorial7_8.Store.dto.delivery.DeliveryCoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static Tutorial7_8.Store.service.delivery.delaySend.DeliveryDelaySendAmqpConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryDelayProducer {

    private final RabbitTemplate rabbit;

    // store the message to delay queue
    public void delayDeliveryRequest(List<DeliveryCoRequest> deliveryCoRequests) {
        log.info("delayDeliveryRequest send to delivery delay queue");
        rabbit.convertAndSend(EX_DELIVERY_DELAY, RK_DELAY, new DeliveryRequestMsg(deliveryCoRequests));
    }

    // optional: send the msg to fail queue for audit
    public void sendFail(DeliveryRequestMsg msg) {
        rabbit.convertAndSend(EX_DELIVERY_DELAY, RK_FAIL, msg);
    }
}
