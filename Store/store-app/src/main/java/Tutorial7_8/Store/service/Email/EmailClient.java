package Tutorial7_8.Store.service.Email;

import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import Tutorial7_8.Store.dto.email.EmailRequest;
import Tutorial7_8.Store.dto.email.EmailResponse;
import Tutorial7_8.Store.error.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@Slf4j
@AllArgsConstructor
public class EmailClient {

    private static final String EMAIL_URL = "http://localhost:8085/api/v1/emails";
    private final ObjectMapper mapper;
    private static final long emailTimeoutMs = 3000L;

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(emailTimeoutMs))
                .build();
    }

    public EmailResponse sendEmailRequest (EmailRequest res){
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(res);

        }catch (Exception e){
            log.warn("Cannot parse email request");
            log.error(e.getMessage());
            throw new BusinessException("JSON_MAPPER_ERROR", "Cannot map the email request", HttpStatus.BAD_REQUEST);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMAIL_URL))
                .timeout(Duration.ofMillis(emailTimeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> resp = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("DeliveryCo Response HTTP non-2xx: " + resp.statusCode());
            }
            return mapper.readValue(resp.body(), EmailResponse.class);
        }catch (Exception e) {
            log.warn("Cannot send email request to EmailService application");
            log.error(e.getMessage());
            throw new BusinessException("EMAIL_SEND_ERROR", "Cannot send email request to EmailService application", HttpStatus.BAD_REQUEST);
        }
    }
}
