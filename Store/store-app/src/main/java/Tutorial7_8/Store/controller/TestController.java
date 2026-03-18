package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.email.EmailRequest;
import Tutorial7_8.Store.service.Email.EmailService;
import Tutorial7_8.Store.service.Email.delaySend.EmailSendProducer;
import Tutorial7_8.Store.service.delivery.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/tests")
@Tag(name = "Test", description = "Relative test API endpoints")
@AllArgsConstructor
public class TestController {
    DeliveryService deliveryService;
    EmailService emailService;
    private final EmailSendProducer producer;


    @GetMapping("/delivery")
    public ResponseEntity<String> deliverOrder() {
        deliveryService.testDelivery();
        return ResponseEntity.ok("test delivered");
    }

    @PostMapping("email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest emailRequest) {
        String res = emailService.sendEmail(emailRequest);

        return ResponseEntity.ok(res);
    }

    @PostMapping("email/mq")
    public ResponseEntity<String> sendEmailByMQ(@RequestBody EmailRequest emailRequest) {
        producer.sendEmail(emailRequest);
        return ResponseEntity.ok("send email mq to queue");
    }



}

