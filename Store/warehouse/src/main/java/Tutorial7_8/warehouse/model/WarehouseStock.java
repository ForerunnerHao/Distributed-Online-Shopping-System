package Tutorial7_8.warehouse.model;

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
        name = "warehouse_stock",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_sku", columnNames = "item_sku"),
        }
)
public class WarehouseStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Version
    private Long version;

    @Column(name = "item_sku", unique = true, nullable = false)
    private String itemSku;

    @Column(name = "total_qty", nullable = false)
    private Integer totalQty;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty;

    @Column(name = "deducted_qty", nullable = false)
    private Integer deductedQty;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Transient
    public int getAvailableQty() {
        return (totalQty == null ? 0 : totalQty)
                - (reservedQty == null ? 0 : reservedQty)
                - (deductedQty == null ? 0 : deductedQty);
    }

    @Transient
    public boolean assertInvariants() {
        int total = totalQty == null ? 0 : totalQty;
        int reserved = reservedQty == null ? 0 : reservedQty;
        int deducted = deductedQty == null ? 0 : deductedQty;
        if (total < 0 || reserved < 0 || deducted < 0) {
            return false;
        }
        return reserved + deducted <= total;
    }

}