package Tutorial7_8.Common.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public enum OutboxStatus {
    PENDING,    // pending delivery
    SENT,       // Delivered successfully
    FAILED      // Failed after multiple retries (requires manual intervention)
}