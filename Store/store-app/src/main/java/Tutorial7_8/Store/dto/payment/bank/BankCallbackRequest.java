package Tutorial7_8.Store.dto.payment.bank;

import Tutorial7_8.Common.enums.TransactionStatus;
import Tutorial7_8.Common.enums.TransactionType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class BankCallbackRequest {
    String transactionRef;
    String orderId;
    TransactionType type;
    TransactionStatus status;
    BigDecimal amount;
    String currency;
    String sourceAccount;
    String destinationAccount;
    String failureReason;
    Instant processedAt;
    Instant occurredAt;
}
