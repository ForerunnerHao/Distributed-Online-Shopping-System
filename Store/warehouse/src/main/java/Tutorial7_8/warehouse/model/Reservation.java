package Tutorial7_8.warehouse.model;

import Tutorial7_8.Common.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;


@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "reservations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reservation_id", columnNames = "reservation_id"),
        },
        indexes = {
                @Index(name = "idx_reservations_expires", columnList = "expires_at"),
                @Index(name = "idx_reservations_status", columnList = "status")
        }
)
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    private Long version;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "item_sku", nullable = false)
    private String itemSku;

    @Column(name = "reservation_id", unique = true, nullable = false)
    private String reservationId;

    @Column(name = "qty", nullable = false)
    private Integer qty;                     // the reservation quality should over 1

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;               // the order's created time + 15min

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override
    public int hashCode() { return getClass().hashCode(); }
}

