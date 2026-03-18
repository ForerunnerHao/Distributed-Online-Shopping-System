package com.delivery.DeliveryCo.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.delivery.DeliveryCo.model.DeliveryInstance;
import com.delivery.DeliveryCo.model.enums.DeliveryStatus;

import java.time.Instant;
import java.util.List;




public interface DeliveryInstanceRepo extends JpaRepository<DeliveryInstance, UUID> {
   // DeliveryInstanceRepo.java
   @Modifying(clearAutomatically = true, flushAutomatically = true)
   @Query("""
   update DeliveryInstance d
      set d.status = :to,
         d.statusUpdatedAt = CURRENT_TIMESTAMP
   where d.status = :from
      and d.type = :type
      and d.statusUpdatedAt <= :cutoff
   """)
   int advanceStatusByType(DeliveryStatus from, DeliveryStatus to, Instant cutoff,
                           com.delivery.DeliveryCo.model.enums.DeliveryType type);

   @Modifying(clearAutomatically = true, flushAutomatically = true)
   @Query("""
   update DeliveryInstance d
      set d.status = :to,
         d.statusUpdatedAt = CURRENT_TIMESTAMP
   where d.status = :from
      and d.type = :toCustomer
      and d.statusUpdatedAt <= :cutoff
      and not exists (
         select 1 from DeliveryInstance p
         where p.orderId = d.orderId
            and p.type = :pickup
            and p.status <> :delivered
      )
   """)
   int advanceStatusToCustomerGated(DeliveryStatus from, DeliveryStatus to, Instant cutoff,
                                    com.delivery.DeliveryCo.model.enums.DeliveryType toCustomer,
                                    com.delivery.DeliveryCo.model.enums.DeliveryType pickup,
                                    DeliveryStatus delivered);

   List<DeliveryInstance> findAllByStatusUpdatedAtAfter(Instant cutoff);

   List<DeliveryInstance> findByOrderId(String orderId);
}
