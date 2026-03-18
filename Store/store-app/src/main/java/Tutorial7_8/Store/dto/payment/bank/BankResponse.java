package Tutorial7_8.Store.dto.payment.bank;

import Tutorial7_8.Common.enums.TransactionStatus;
import Tutorial7_8.Common.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class BankResponse {
    String transactionRef;
    String orderId;
    // PAYMENT / REFUND
    TransactionType type;
    TransactionStatus status;
    BigDecimal amount;
    String currency;
    String sourceAccount;
    String destinationAccount;
    String description;
    String failureReason;
    Instant processedAt;
    Instant updatedAt;
}
