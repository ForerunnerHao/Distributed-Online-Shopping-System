package Tutorial7_8.Store.dto.delivery;

import Tutorial7_8.Common.enums.DeliveryType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeliveryCoRequest {
    private String detail;
    private String fromAddress;
    private String orderId;
    private String toAddress;
    private DeliveryType type;
}
