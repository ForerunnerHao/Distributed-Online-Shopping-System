package Tutorial7_8.Store.service.Email.delaySend;

import Tutorial7_8.Store.dto.email.EmailRequest;
import Tutorial7_8.Store.service.Email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static Tutorial7_8.Store.service.Email.delaySend.EmailSendAmqpConfig.ES_MAIN;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSendConsumer {

    private final EmailService emailService;
    private final EmailSendProducer producer;

    @RabbitListener(queues = ES_MAIN)
    public void parseDeliveryResponse(EmailRequest msg){
        log.info(msg.toString());
        try {
            log.info("get the email send message from the queue");
            emailService.sendEmail(msg);

        }catch (Exception e){
            log.error(e.getMessage());
            producer.sendFail(msg);
        }
    }
}
