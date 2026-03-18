package Tutorial7_8.Store.dto.order;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCreateRequest {
    @NotBlank
    private Long item_id;

    @NotBlank
    private int quantity;
}
