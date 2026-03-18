package com.delivery.DeliveryCo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import com.delivery.DeliveryCo.model.enums.DeliveryStatus;
import com.delivery.DeliveryCo.model.enums.DeliveryType;

/**Model instance to define a Delivery 
 * 
 * params:
 * id, details, status, type, fromAddress, toAddress
 * 
 * 
 * 
*/

@Getter
@Setter
@NoArgsConstructor
@Entity
public class DeliveryInstance{
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    private String details;

    @Column(nullable = false)
    private String orderId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant statusUpdatedAt;

    @Version
    private long version;

    @Enumerated(EnumType.STRING)
    @Column(length=16, nullable = false)
    private DeliveryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length=16, nullable = false)
    private DeliveryType type;

    @Column(nullable = false)
    private String toAddress;

    @Column(nullable=false)
    private String fromAddress;


    
    public DeliveryInstance(String details, DeliveryStatus status, DeliveryType type, 
    String toAddress, String fromAddress, String orderId){
        this.details = details;
        this.status = status;
        this.type = type;
        
        if (this.type == DeliveryType.WAREHOUSE_PICKUP){
            this.toAddress="1A Delivery Headquarter, DeliveryCo";
            this.fromAddress=fromAddress;
        }else{
            this.toAddress = toAddress;
            this.fromAddress = fromAddress;
        }
        this.orderId = orderId;
    }


    @PrePersist
    public void prePersist() {
        if (statusUpdatedAt == null) statusUpdatedAt = Instant.now();
        if (status == null) status = DeliveryStatus.PLACED;
    }
}