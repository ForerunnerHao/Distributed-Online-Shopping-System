package Tutorial7_8.Store.service.delivery.statusUpdate;

import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DeliveryResponseMsg {
    private String orderId;
    private Instant occurredAt;
    private List<Item> updates;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String deliveryId;
        private String type;         // WAREHOUSE_PICKUP / TO_CUSTOMER
        private String status;       // PLACED / PREPARING / DELIVERING / DELIVERED / LOST
        private Instant statusUpdatedAt;
        private String details;
        private String fromAddress;
        private String toAddress;
    }
}