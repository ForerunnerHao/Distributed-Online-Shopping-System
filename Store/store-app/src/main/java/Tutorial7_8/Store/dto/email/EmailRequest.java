package Tutorial7_8.Store.dto.email;

import Tutorial7_8.Common.enums.EventType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EmailRequest {
    String to;
    String subject;
    String body;
    String orderId;
    EventType eventType;
}
