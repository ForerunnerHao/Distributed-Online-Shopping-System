package Tutorial7_8.Store.dto.order;

import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Common.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDTO {
    Long id;
    Long user_id;
    Long item_id;
    String item_sku;
    int quantity;
    List<Order.WarehouseReservation> warehouse_reservations;
    BigDecimal total_amount;
    OrderStatus status;
    Instant created_at;
    Instant expires_at;
    Instant updated_at;
}
