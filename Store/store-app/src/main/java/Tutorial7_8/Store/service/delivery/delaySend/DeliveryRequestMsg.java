package Tutorial7_8.Store.service.delivery.delaySend;

import Tutorial7_8.Store.dto.delivery.DeliveryCoRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequestMsg {
    List<DeliveryCoRequest> deliveryCoRequests;
}
