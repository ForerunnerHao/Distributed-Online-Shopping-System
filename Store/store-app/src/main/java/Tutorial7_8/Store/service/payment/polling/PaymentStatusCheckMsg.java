package Tutorial7_8.Store.service.payment.polling;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusCheckMsg {
    private Long orderId;
    private String idempotencyKey;
    private int attempt; // retry times：0,1,2
}

