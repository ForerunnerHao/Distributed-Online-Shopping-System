package Tutorial7_8.Store.service.payment.polling;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentPollingAmqpConfig {

    public static final String EX      = "pay.status.ex";
    public static final String RK_CHECK= "check";
    public static final String RK_FAIL = "fail";
    public static final String RK_R1   = "retry1";
    public static final String RK_R2   = "retry2";
    public static final String RK_R3   = "retry3";

    public static final String Q_MAIN  = "pay.status.q";
    public static final String Q_FAIL  = "pay.status.q.fail";
    public static final String Q_R1    = "pay.status.q.retry1"; // 3s
    public static final String Q_R2    = "pay.status.q.retry2"; // 6s
    public static final String Q_R3    = "pay.status.q.retry3"; // 9s

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(EX, true, false);
    }


    @Bean
    public Queue mainQ() {
        return QueueBuilder.durable(Q_MAIN).build();
    }
    @Bean
    public Binding mainBind(TopicExchange paymentExchange) {
        return BindingBuilder.bind(mainQ()).to(paymentExchange).with(RK_CHECK);
    }

    @Bean
    public Queue retry1Q() {
        return QueueBuilder.durable(Q_R1)
                .withArgument("x-dead-letter-exchange", EX)
                .withArgument("x-dead-letter-routing-key", RK_CHECK)
                .withArgument("x-message-ttl", 3000) // 3s
                .build();
    }
    @Bean
    public Binding retry1Bind(TopicExchange paymentExchange) {
        return BindingBuilder.bind(retry1Q()).to(paymentExchange).with(RK_R1);
    }

    @Bean
    public Queue retry2Q() {
        return QueueBuilder.durable(Q_R2)
                .withArgument("x-dead-letter-exchange", EX)
                .withArgument("x-dead-letter-routing-key", RK_CHECK)
                .withArgument("x-message-ttl", 6000)
                .build();
    }
    @Bean
    public Binding retry2Bind(TopicExchange paymentExchange) {
        return BindingBuilder.bind(retry2Q()).to(paymentExchange).with(RK_R2);
    }

    @Bean
    public Queue retry3Q() {
        return QueueBuilder.durable(Q_R3)
                .withArgument("x-dead-letter-exchange", EX)
                .withArgument("x-dead-letter-routing-key", RK_CHECK)
                .withArgument("x-message-ttl", 9000)
                .build();
    }
    @Bean
    public Binding retry3Bind(TopicExchange paymentExchange) {
        return BindingBuilder.bind(retry3Q()).to(paymentExchange).with(RK_R3);
    }

    @Bean
    public Queue failQ() {
        return QueueBuilder.durable(Q_FAIL).build();
    }
    @Bean
    public Binding failBind(TopicExchange paymentExchange) {
        return BindingBuilder.bind(failQ()).to(paymentExchange).with(RK_FAIL);
    }
}

