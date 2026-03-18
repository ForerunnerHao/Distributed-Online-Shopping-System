package Tutorial7_8.Store.dto.payment.bank;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BankRequest {
    @NotBlank
    @Size(max = 32)
    private String sourceAccount;

    @NotBlank
    @Size(max = 32)
    @Builder.Default
    private String destinationAccount = "STORE-0001";

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "AUD";

    @Size(max = 64)
    private String orderId;

    @Size(max = 512)
    private String description;

    @Size(max = 512)
    @Builder.Default
    private String callbackUrl = "http://localhost:8080/api/payments/callback";

    @Size(max = 128)
    private String idempotencyKey;

    @Builder.Default
    private boolean simulateFailure = false;
}
