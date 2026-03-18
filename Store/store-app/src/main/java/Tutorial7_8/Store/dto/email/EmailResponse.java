package Tutorial7_8.Store.dto.email;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailResponse {
    private String id;
    private String orderId;
    private String toEmail;
    private String eventType;
    private String subject;
    private String status;
    private String providerResponse;
    private String createdAt;
    private String sentAt;
}
