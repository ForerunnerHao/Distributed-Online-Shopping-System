package Tutorial7_8.Store.dto.item;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemDTO {
    Long id;
    String name;
    String sku;
    BigDecimal price;
    @Builder.Default
    List<ItemWarehouseDTO> itemWarehouseList = List.of();
}

