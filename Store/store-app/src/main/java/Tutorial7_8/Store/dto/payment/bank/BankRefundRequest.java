package Tutorial7_8.Store.dto.payment.bank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BankRefundRequest {
    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    @Size(max = 512)
    @Builder.Default
    private String callbackUrl = "http:localhost:8080/api/payment/refund/callback";

    @Size(max = 256)
    private String reason;
}
