package Tutorial7_8.Store.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentRequest {
    Long orderId;
    String customerAccount;
    BigDecimal amount;
    boolean simulateFailure;
}
