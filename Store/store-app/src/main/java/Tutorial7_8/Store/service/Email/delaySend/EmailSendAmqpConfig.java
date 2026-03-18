package Tutorial7_8.Store.service.Email.delaySend;

import lombok.AllArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor

public class EmailSendAmqpConfig {

    // exchange
    public static final String EX_EMAIL_SEND = "email.send.ex";

    // router
    public static final String RK_ES_SEND = "send";
    public static final String RK_ES_FAIL = "fail";

    // queue
    public static final String ES_MAIN = "email.send.q";
    public static final String ES_FAIL = "email.send.q.fail";

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(EX_EMAIL_SEND, true, false);
    }

    @Bean
    public Queue mainEmailQueue() {
        return QueueBuilder.durable(ES_MAIN).build();
    }

    @Bean
    public Binding mainEmailBind(TopicExchange emailExchange) {
        return BindingBuilder.bind(mainEmailQueue()).to(emailExchange).with(RK_ES_SEND);
    }

    @Bean
    public Queue mainEmailFailQueue() {
        return QueueBuilder.durable(ES_FAIL).build();
    }

    @Bean
    public Binding mainEmailFailBind(TopicExchange emailExchange) {
        return BindingBuilder.bind(mainEmailFailQueue()).to(emailExchange).with(RK_ES_FAIL);
    }
}
