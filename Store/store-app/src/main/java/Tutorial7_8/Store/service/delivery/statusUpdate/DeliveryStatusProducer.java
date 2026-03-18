package Tutorial7_8.Store.service.delivery.statusUpdate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static Tutorial7_8.Store.service.delivery.statusUpdate.DeliveryStatusSendAmqpConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStatusProducer {

    private final RabbitTemplate rabbit;

    // optional: send the msg to fail queue for audit
    public void sendFail(DeliveryResponseMsg msg) {
        rabbit.convertAndSend(EX_DELIVERY_UPDATE, RK_DU_FAIL, msg);
    }

}
