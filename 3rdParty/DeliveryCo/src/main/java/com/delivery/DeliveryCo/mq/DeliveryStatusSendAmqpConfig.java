package com.delivery.DeliveryCo.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;

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
    public Queue mainDeliveryQueue() {
        return QueueBuilder.durable(DU_MAIN).build();
    }

    @Bean
    public Binding mainDeliveryBind(TopicExchange deliveryUpdateExchange) {
        return BindingBuilder.bind(mainDeliveryQueue()).to(deliveryUpdateExchange).with(RK_DU_SEND);
    }

    @Bean
    public Queue deliveryFailQueue() {
        return QueueBuilder.durable(DU_FAIL).build();
    }

    @Bean
    public Binding deliveryFailBind(TopicExchange deliveryUpdateExchange) {
        return BindingBuilder.bind(deliveryFailQueue()).to(deliveryUpdateExchange).with(RK_DU_FAIL);
    }

}
