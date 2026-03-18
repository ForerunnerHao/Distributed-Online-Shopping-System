package com.delivery.DeliveryCo.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;


import static com.delivery.DeliveryCo.mq.DeliveryStatusSendAmqpConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStatusProducer {
    private final RabbitTemplate rabbit;

    // store the message to delay queue
    public void updateDeliveryStatus(DeliveryResponseMsg msg) {
        log.info("delayDeliveryRequest send to delivery delay queue");
        rabbit.convertAndSend(EX_DELIVERY_UPDATE, RK_DU_SEND, msg);
    }
}
