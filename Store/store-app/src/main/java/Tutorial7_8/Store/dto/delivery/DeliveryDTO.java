package Tutorial7_8.Store.dto.delivery;

import Tutorial7_8.Store.service.delivery.statusUpdate.DeliveryResponseMsg;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Builder
@Getter
@Setter
public class DeliveryDTO {
    private Long id;
    private String orderId;
    private List<DeliveryDTO.Package> packages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Package {
        private String deliveryId;
        private String type;         // WAREHOUSE_PICKUP / TO_CUSTOMER
        private String status;       // PLACED / PREPARING / DELIVERING / DELIVERED / LOST
        private Instant statusUpdatedAt;
        private String details;
        private String fromAddress;
        private String toAddress;
    }
}
