package Tutorial7_8.Store.service.Email;

import Tutorial7_8.Store.dto.email.EmailRequest;
import Tutorial7_8.Store.error.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@AllArgsConstructor
public class EmailService {
    private final EmailClient emailClient;

    public String sendEmail(EmailRequest email) {
        log.info("Sending email");
        try {
            return emailClient.sendEmailRequest(email).getId();
        }catch (Exception e) {
            log.warn("Send Email Failed", e);
            throw new BusinessException("EMAIL_SEND_FAIL", "Can not send email", HttpStatus.BAD_REQUEST);
        }
    }
}
