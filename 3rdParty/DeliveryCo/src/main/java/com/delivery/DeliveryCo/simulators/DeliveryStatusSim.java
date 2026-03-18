package com.delivery.DeliveryCo.simulators;

import java.time.Instant;

import com.delivery.DeliveryCo.model.DeliveryInstance;
import com.delivery.DeliveryCo.mq.DeliveryResponseMsg;
import com.delivery.DeliveryCo.mq.DeliveryStatusProducer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import com.delivery.DeliveryCo.model.enums.DeliveryStatus;
import com.delivery.DeliveryCo.model.enums.DeliveryType;
import com.delivery.DeliveryCo.repository.DeliveryInstanceRepo;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Status update simulation via Scheduled task
 */
@Service
@RequiredArgsConstructor
public class DeliveryStatusSim {
    private final DeliveryInstanceRepo repo;
    private final DeliveryStatusProducer producer;

    // DeliveryStatusSim.java
    @SchedulerLock(name = "deliveryStatusTick", lockAtMostFor = "PT8S", lockAtLeastFor = "PT2S")
    @Transactional
    @Scheduled(fixedDelay = 8_000, initialDelay = 2_000)
    public void tick() {
        Instant cutoff = Instant.now().minusSeconds(8);

        // 1) Progress pickups independently
        repo.advanceStatusByType(DeliveryStatus.PLACED, chanceToFaiDeliveryStatus(DeliveryStatus.PREPARING), cutoff, DeliveryType.WAREHOUSE_PICKUP);
        repo.advanceStatusByType(DeliveryStatus.PREPARING, chanceToFaiDeliveryStatus(DeliveryStatus.DELIVERING), cutoff, DeliveryType.WAREHOUSE_PICKUP);
        repo.advanceStatusByType(DeliveryStatus.DELIVERING, chanceToFaiDeliveryStatus(DeliveryStatus.DELIVERED), cutoff, DeliveryType.WAREHOUSE_PICKUP);

        // 2) Progress to-customer only when all pickups delivered
        repo.advanceStatusToCustomerGated(DeliveryStatus.PLACED, chanceToFaiDeliveryStatus(DeliveryStatus.PREPARING), cutoff, DeliveryType.TO_CUSTOMER, DeliveryType.WAREHOUSE_PICKUP, DeliveryStatus.DELIVERED);
        repo.advanceStatusToCustomerGated(DeliveryStatus.PREPARING, chanceToFaiDeliveryStatus(DeliveryStatus.DELIVERING), cutoff, DeliveryType.TO_CUSTOMER, DeliveryType.WAREHOUSE_PICKUP, DeliveryStatus.DELIVERED);
        repo.advanceStatusToCustomerGated(DeliveryStatus.DELIVERING, chanceToFaiDeliveryStatus(DeliveryStatus.DELIVERED), cutoff, DeliveryType.TO_CUSTOMER, DeliveryType.WAREHOUSE_PICKUP, DeliveryStatus.DELIVERED);

        //        send MQ to stock app
        // TODO ??? producer.updateDeliveryStatus();
        var changed = repo.findAllByStatusUpdatedAtAfter(cutoff);

        // 按订单聚合发消息（一个订单一条消息）
        changed.stream()
                .collect(Collectors.groupingBy(DeliveryInstance::getOrderId))
                .forEach((orderId, list) -> {
                    DeliveryResponseMsg msg = DeliveryResponseMsg.builder()
                            .orderId(String.valueOf(orderId))
                            .occurredAt(Instant.now())
                            .updates(list.stream()
                                    .map(d -> DeliveryResponseMsg.Item.builder()
                                            .deliveryId(String.valueOf(d.getId()))
                                            .type(d.getType().name())
                                            .status(d.getStatus().name())
                                            .statusUpdatedAt(d.getStatusUpdatedAt())
                                            .details(d.getDetails())
                                            .fromAddress(d.getFromAddress())
                                            .toAddress(d.getToAddress())
                                            .build())
                                    .toList())
                            .build();
                    producer.updateDeliveryStatus(msg);
                });
    }

    /**
     * 10% chance for any stage of delivery status (except for PLACED) to become LOST
     */
    public DeliveryStatus chanceToFaiDeliveryStatus(DeliveryStatus status) {

        return ThreadLocalRandom.current().nextInt(10) == 0 ? DeliveryStatus.LOST : status;
    }
}
