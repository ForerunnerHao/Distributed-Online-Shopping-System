package Tutorial7_8.Store.model;

import Tutorial7_8.Common.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@ToString(exclude = "user")
@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_status_created_at", columnList = "status, created_at")
        }
)
public class Order {

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
            foreignKey = @ForeignKey(name = "fk_orders_user") // ← 外键约束名
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_item"))
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING) // store the string to avoid the reorder error
    @Builder.Default
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status = OrderStatus.CREATED;

    // let backend service add the created time and update time
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "warehouse_reservations", columnDefinition = "jsonb")
    @Builder.Default
    private List<WarehouseReservation> warehouseReservations = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WarehouseReservation {
        private String reservationId;     // {orderId}-wh1
        private String warehouseCode;     // wh1 / wh2
        private Integer reservedQty;      //
        private String status;            // PENDING/RELEASED/CONFIRMED
    }

    /*
        Java equals() and hashCode() Contracts
        http://baeldung.com/java-equals-hashcode-contracts
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
