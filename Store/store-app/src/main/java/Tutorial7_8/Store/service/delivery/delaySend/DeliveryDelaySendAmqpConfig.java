package Tutorial7_8.Store.service.delivery.delaySend;

import lombok.AllArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class DeliveryDelaySendAmqpConfig {
    // exchange
    public static final String EX_DELIVERY_DELAY      = "delivery.delay.ex";

    // router
    public static final String RK_SEND= "send";
    public static final String RK_FAIL = "fail";
    public static final String RK_DELAY   = "delivery.delay";

    // queue
    public static final String DD_MAIN  = "delivery.delay.q";
    public static final String DD_FAIL  = "delivery.delay.q.fail";
    public static final String DD_10Q    = "delivery.delay.q.delay10";


    @Bean
    public TopicExchange deliveryExchange() {
        return new TopicExchange(EX_DELIVERY_DELAY, true, false);
    }


    @Bean
    public Queue mainDeliveryQueue() {
        return QueueBuilder.durable(DD_MAIN).build();
    }
    @Bean
    public Binding mainDeliveryBind(TopicExchange deliveryExchange) {
        return BindingBuilder.bind(mainDeliveryQueue()).to(deliveryExchange).with(RK_SEND);
    }

    // no consumer will get the message from this queue,
    // so when it died the msg will resend to RK_SEND queue which our request sending service are listening
    @Bean
    public Queue deliveryDelayQueue() {
        return QueueBuilder.durable(DD_10Q)
                .withArgument("x-dead-letter-exchange", EX_DELIVERY_DELAY)
                .withArgument("x-dead-letter-routing-key", RK_SEND)
                .withArgument("x-message-ttl", 10000) // 10s
                .build();
    }
    @Bean
    public Binding deliveryDelayBind(TopicExchange deliveryExchange) {
        return BindingBuilder.bind(deliveryDelayQueue()).to(deliveryExchange).with(RK_DELAY);
    }

    @Bean
    public Queue deliveryFailQueue() {
        return QueueBuilder.durable(DD_FAIL).build();
    }
    @Bean
    public Binding deliveryFailBind(TopicExchange deliveryExchange) {
        return BindingBuilder.bind(deliveryFailQueue()).to(deliveryExchange).with(RK_FAIL);
    }
}
