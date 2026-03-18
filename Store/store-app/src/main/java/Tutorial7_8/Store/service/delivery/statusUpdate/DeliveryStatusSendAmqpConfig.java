package Tutorial7_8.Store.service.delivery.statusUpdate;

import lombok.AllArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor

public class DeliveryStatusSendAmqpConfig {

    // exchange
    public static final String EX_DELIVERY_UPDATE = "delivery.update.ex";

    // router
    public static final String RK_DU_SEND = "send";
    public static final String RK_DU_FAIL = "fail";

    // queue
    public static final String DU_MAIN = "delivery.update.q";
    public static final String DU_FAIL = "delivery.update.q.fail";

    @Bean
    public TopicExchange deliveryUpdateExchange() {
        return new TopicExchange(EX_DELIVERY_UPDATE, true, false);
    }

    @Bean
    public Queue mainDeliveryUpdateQueue() {
        return QueueBuilder.durable(DU_MAIN).build();
    }

    @Bean
    public Binding mainDeliveryUpdateBind(TopicExchange deliveryUpdateExchange) {
        return BindingBuilder.bind(mainDeliveryUpdateQueue()).to(deliveryUpdateExchange).with(RK_DU_SEND);
    }

    @Bean
    public Queue deliveryUpdateFailQueue() {
        return QueueBuilder.durable(DU_FAIL).build();
    }

    @Bean
    public Binding deliveryUpdateFailBind(TopicExchange deliveryUpdateExchange) {
        return BindingBuilder.bind(deliveryUpdateFailQueue()).to(deliveryUpdateExchange).with(RK_DU_FAIL);
    }
}
