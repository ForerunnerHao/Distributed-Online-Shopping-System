package Tutorial7_8.Store.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundRequest {

    @NotBlank
    private Long orderId;

    @Size(max = 256)
    private String reason;

}
