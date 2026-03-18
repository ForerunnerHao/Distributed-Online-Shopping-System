package com.delivery.DeliveryCo.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.delivery.DeliveryCo.dto.DeliveryInstanceDTO;
import com.delivery.DeliveryCo.model.DeliveryInstance;
import com.delivery.DeliveryCo.model.enums.DeliveryStatus;
import com.delivery.DeliveryCo.model.enums.DeliveryType;
import com.delivery.DeliveryCo.repository.DeliveryInstanceRepo;
import com.delivery.DeliveryCo.utils.Helpers;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class DeliveryInstanceService {
    private final DeliveryInstanceRepo diRepo;

    @Autowired
    public DeliveryInstanceService(DeliveryInstanceRepo diRepo){
        this.diRepo = diRepo;
    }

    @Transactional
    public DeliveryInstanceDTO create(String details, DeliveryStatus status, DeliveryType type, String toAddress, String fromAddress, String order_id){
        DeliveryInstance di = new DeliveryInstance(details, status, type, toAddress, fromAddress, order_id);
        di = diRepo.save(di);
        return new DeliveryInstanceDTO(di);
    }

    @Transactional
    public DeliveryInstanceDTO get(String id){
        UUID uuid = Helpers.StringtoUUID(id);
        if (uuid == null) return null;
        return diRepo.findById(uuid)
                        .map(DeliveryInstanceDTO::new)
                        .orElse(null);

    }
    /**Update for Delivery instance
     * @param id String of the object id
     * @param diDTO DeliveryInstanceDTO instance, valid fields for update are
     * details, status, toAddress
     */
    @Transactional
    public DeliveryInstanceDTO update(String id, DeliveryInstanceDTO diDTO){
        UUID uuid = Helpers.StringtoUUID(id);
        DeliveryInstance di = diRepo.findById(uuid).orElseThrow(
            () -> new EntityNotFoundException("DeliveryInstance" + id));
        
        if(diDTO.getDetails() != null) di.setDetails(diDTO.getDetails());
        if (diDTO.getToAddress() != null) di.setToAddress(diDTO.getToAddress());
        if (diDTO.getStatus() != null && diDTO.getStatus() == DeliveryStatus.CANCELLED) di.setStatus(DeliveryStatus.CANCELLED);

        return new DeliveryInstanceDTO(di);

    }

    @Transactional
    public List<DeliveryInstanceDTO> getDeliveryByOrder(String orderId){
        List<DeliveryInstance> instances = diRepo.findByOrderId(orderId);
        ArrayList<DeliveryInstanceDTO> dtos = new ArrayList<>();

        for (DeliveryInstance di : instances){
            dtos.add(new DeliveryInstanceDTO(di));
        }
        return dtos;
    }

    // @Transactional
    // public DeliveryInstanceDTO delete(String id){
        
    // }
}
