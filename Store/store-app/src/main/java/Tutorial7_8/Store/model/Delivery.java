package Tutorial7_8.Store.model;


import Tutorial7_8.Common.enums.DeliveryStatus;
import Tutorial7_8.Common.enums.DeliveryType;
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
@ToString(exclude = {"order", "user"})
@Entity
@Table(
        name = "delivery",
        indexes = {
                @Index(name = "idx_deliveries_user_id", columnList = "user_id"),
                @Index(name = "idx_deliveries_order_id", columnList = "order_id")
        }
)
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Postgres/MySQL
    @Column(name = "id")
    private Long id;

    // @Version is JPA optimistic locking version number field
    @Version
    private Long version;

    // many deliveries belong to a user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deliveries_user") // ← 外键约束名
    )
    private User user;

    // One delivery belong to an order
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_deliveries_order") // ← 外键约束名
    )
    private Order order;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_information_list", columnDefinition = "jsonb")
    @Builder.Default
    private List<Delivery.DeliveryInformation> deliveryInformationList = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryInformation {
        private String details;
        private String fromAddress;
        private String id;
        private DeliveryStatus status;
        private String toAddress;
        private DeliveryType type;
        private Instant statusUpdatedAt;
    }
}
