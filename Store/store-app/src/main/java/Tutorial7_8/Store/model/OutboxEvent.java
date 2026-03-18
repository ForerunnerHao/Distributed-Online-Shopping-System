package Tutorial7_8.Store.model;

import Tutorial7_8.Common.enums.OutboxStatus;
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
        name = "outbox_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_outbox_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_outbox_status_next", columnList = "status, next_retry_at"),
                @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type, aggregate_id")
        }
)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    private Long version;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;         // like "Order", "Payment", "Delivery"

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;             // like "DELIVERY_REQUESTED"

    // Postgres: jsonb
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;          // Next retry time after exponential backoff

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;        // idempotent keys (like orderId + eventType + hash(payload))

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutboxEvent other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override
    public int hashCode() { return getClass().hashCode(); }
}
