package Tutorial7_8.Store.service.delivery.statusUpdate;

import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import Tutorial7_8.Store.model.Delivery;
import Tutorial7_8.Store.service.delivery.DeliveryTxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

import static Tutorial7_8.Store.service.delivery.statusUpdate.DeliveryStatusSendAmqpConfig.DU_MAIN;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatusConsumer {

    private final DeliveryStatusProducer producer;
    private final DeliveryTxService txService;

    @RabbitListener(queues = DU_MAIN)
    public void parseDeliveryResponse(DeliveryResponseMsg msg){
        log.info(msg.toString());
        try {
            log.info("Get {} response(s)  from DeliveryCo", msg.getUpdates().size());
            // update the order and delivery status
            txService.updateDeliveryStatusInTransaction(msg);

        }catch (Exception e){
            log.error(e.getMessage());
            producer.sendFail(msg);
        }

    }
}
