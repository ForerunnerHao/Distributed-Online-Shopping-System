package com.delivery.DeliveryCo.controllers;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.delivery.DeliveryCo.dto.DeliveryInstanceDTO;
import com.delivery.DeliveryCo.services.DeliveryInstanceService;

import jakarta.validation.Valid;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.delivery.DeliveryCo.model.enums.DeliveryStatus;
import com.delivery.DeliveryCo.model.enums.DeliveryType;


@Slf4j
@RestController
@RequestMapping("api/rest/v1/delivery")
public class DeliveryInstanceController {
    private final DeliveryInstanceService deliService;
    public DeliveryInstanceController(DeliveryInstanceService deliService){
        this.deliService = deliService;
    }

    @PostMapping(path = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DeliveryInstanceDTO>> createMany(
        @Valid @RequestBody List<CreateDeliveryRequest> reqs
    ) {
        log.info("[api/rest/v1/delivery/batch] Received request to create deliveries");

        Integer count_TO_CUSTOMER = 0;
        List<DeliveryInstanceDTO> res = new ArrayList<>();
        for (CreateDeliveryRequest cdr : reqs) {
            //Check if there are more than 1 TO_CUSTOMER type of delivery. If it does, return Bad Request.
            if (count_TO_CUSTOMER > 1) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ArrayList<DeliveryInstanceDTO>());
            
            if (cdr.type == DeliveryType.TO_CUSTOMER){
                count_TO_CUSTOMER += 1;
            }
            res.add(deliService.create(
                cdr.detail,
                DeliveryStatus.PLACED,
                cdr.type,
                cdr.toAddress,
                cdr.fromAddress,
                cdr.orderId
            ));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
    @PostMapping
    public ResponseEntity<DeliveryInstanceDTO> create(@Valid @RequestBody CreateDeliveryRequest req) {
        log.info("Create delivery instance: {}", req.toString());
        DeliveryInstanceDTO dto = deliService.create(
            req.detail,
            DeliveryStatus.PLACED,
            req.type, 
            req.toAddress,
            req.fromAddress,
            req.orderId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryInstanceDTO> getDelivery(@PathVariable String deliveryId) {
        DeliveryInstanceDTO dto = deliService.get(deliveryId);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }
    @GetMapping("/orderId/{orderId}")
    public ResponseEntity<Collection<DeliveryInstanceDTO>> getDeliveryByOrder(@PathVariable String orderId) {
        List<DeliveryInstanceDTO> dtos = deliService.getDeliveryByOrder(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }

    @ToString
    public static class CreateDeliveryRequest {
    public String detail;
    public DeliveryType type;
    public String toAddress;
    public String fromAddress;
    public String orderId;
    public CreateDeliveryRequest() {}
    }

    
}
