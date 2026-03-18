package Tutorial7_8.Common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReservationStatus {
    PENDING,        // init default value
    RESERVED,       // Reserved, waiting for payment or confirmation of deduction
    CONFIRMED,      // Confirmed deduction (confirmed after successful payment)
    RELEASED,       // Released (timeout/cancellation/payment failure)
    EXPIRED,         // Timeout reservation for timer recycling
    ERROR,
}
