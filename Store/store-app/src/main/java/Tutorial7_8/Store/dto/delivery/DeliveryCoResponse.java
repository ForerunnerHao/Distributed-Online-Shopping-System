package Tutorial7_8.Store.dto.delivery;

import Tutorial7_8.Common.enums.DeliveryStatus;
import Tutorial7_8.Common.enums.DeliveryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Getter
@Setter
@Builder
@ToString
public class DeliveryCoResponse {
    private String details;
    private String fromAddress;
    private String id;
    private String orderId;
    private DeliveryStatus status;
    private String toAddress;
    private DeliveryType type;
    private Instant statusUpdatedAt;

}
