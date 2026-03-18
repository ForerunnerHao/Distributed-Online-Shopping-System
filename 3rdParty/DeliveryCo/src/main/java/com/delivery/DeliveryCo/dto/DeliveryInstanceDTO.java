package com.delivery.DeliveryCo.dto;

import java.util.UUID;

import com.delivery.DeliveryCo.model.DeliveryInstance;
import com.delivery.DeliveryCo.model.enums.DeliveryStatus;
import com.delivery.DeliveryCo.model.enums.DeliveryType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(Include.NON_EMPTY)
public class DeliveryInstanceDTO {
    private UUID id;
    private String details;
    private DeliveryStatus status;
    private DeliveryType type;
    private String toAddress;
    private String fromAddress;
    private String orderId;

    public DeliveryInstanceDTO(DeliveryInstance di){
        this.id = di.getId();
        this.details = di.getDetails();
        this.status = di.getStatus();
        this.type= di.getType();
        this.toAddress=di.getToAddress();
        this.fromAddress=di.getFromAddress();
        this.orderId=di.getOrderId();
    }

}
