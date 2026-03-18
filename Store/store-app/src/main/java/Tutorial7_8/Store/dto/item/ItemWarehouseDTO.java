package Tutorial7_8.Store.dto.item;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ItemWarehouseDTO {
    String warehouseName;
    String warehouseCode;
    int totalQty;
    int reservedQty;
    int deductedQty;
}