package Tutorial7_8.Store.model;

import Tutorial7_8.Common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@ToString(exclude = {"user", "order"})
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payments_user_id", columnList = "user_id"),
                @Index(name = "idx_payments_order_id", columnList = "order_id"),
        }
)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres/MySQL
    @Column(name = "id")
    private Long id;

    // @Version is JPA optimistic locking version number field
    @Version
    private Long version;

    // many orders belong to a user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_user") // ← 外键约束名
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Enumerated(EnumType.STRING) // store the string to avoid the reorder error
    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private PaymentStatus status = PaymentStatus.CREATED;

    // let backend service add the created time and update time
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "source_account", nullable = false)
    private String sourceAccount;

    @Column(name = "destination_account", nullable = false)
    @Builder.Default
    private String destinationAccount = "STORE-0001";

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "refund_transaction_ref")
    private String refundTransactionRef;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "AUD";

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    /*
        Java equals() and hashCode() Contracts
        http://baeldung.com/java-equals-hashcode-contracts
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
