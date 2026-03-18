package Tutorial7_8.Store.dto.warehouse;

import Tutorial7_8.Common.enums.InventoryResponseStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class InventoryResponse {
    InventoryResponseStatus status;
    String message;
    @NotBlank
    private final Long orderId;
    @NotBlank
    private final String itemSku;
}
