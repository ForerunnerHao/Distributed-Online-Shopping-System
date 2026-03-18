package Tutorial7_8.Store.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentDTO {
    Long paymentId;
    Long orderId;
    Long userId;
    String transactionRef;
    String refundTransactionRef;
    String paymentType;
    String paymentStatus;
    BigDecimal paymentAmount;
    Instant orderCreateDate;
    Instant orderExpiredDate;
    Instant paymentDate;
    String message;
}
