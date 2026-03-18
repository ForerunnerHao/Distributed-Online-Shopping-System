package Tutorial7_8.Store.service.delivery;

import Tutorial7_8.Common.enums.DeliveryStatus;
import Tutorial7_8.Common.enums.DeliveryType;
import Tutorial7_8.Common.enums.OrderStatus;
import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Delivery;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.repository.DeliveryRepository;
import Tutorial7_8.Store.repository.OrderRepository;
import Tutorial7_8.Store.service.Email.delaySend.EmailSendProducer;
import Tutorial7_8.Store.service.InventoryService;
import Tutorial7_8.Store.service.delivery.statusUpdate.DeliveryResponseMsg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryTxService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final InventoryService inventoryService;
    private final DeliveryService deliveryService;

    private final EmailSendProducer emailSendProducer;

    @Transactional
    public void updateOrderStatusInTransaction(Long orderId, OrderStatus status) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public OrderStatus getOderStatusInTransaction(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(
                () -> {
                    log.warn("Order not found for orderId: {}", orderId);
                    return new RuntimeException("Order not found for orderId: " + orderId);
                }
        );
        return order.getStatus();
    }

    @Transactional
    public void handleSuccessResponseInTransaction(Long orderId, List<DeliveryCoResponse> responses) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
        deliveryService.processDeliveryResponse(responses);
        order.setStatus(OrderStatus.SHIPMENT_ACCEPTED);
        orderRepository.save(order);

        List<Delivery.DeliveryInformation> deliveryInformationList = updateDelivery(responses);

        Optional<Delivery> delivery = deliveryRepository.findByOrderIdForUpdate(orderId);
        if (delivery.isEmpty()) {
            log.warn("[handleSuccessResponseInTransaction] Can not find Delivery by order {}", orderId);
            throw new RuntimeException("Can not find Delivery by order: " + orderId);
        }
        delivery.get().setDeliveryInformationList(deliveryInformationList);
        deliveryRepository.save(delivery.get());
    }

    @Transactional
    public void handleFailureInTransaction(Long orderId, Exception e) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();
        order.setStatus(OrderStatus.CANCELED_BY_SYSTEM);
        orderRepository.save(order);

        boolean ok = inventoryService.releaseItemForOrder(orderId);
        if (!ok) {
            log.warn("Release stock failed for order {}", orderId);
        }
        log.error("Delivery request failed for order {}: {}", orderId, e.getMessage(), e);
    }

    @Transactional
    public void createNewDeliveryInTransaction(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow();

        List<Delivery.DeliveryInformation> deliveryInformationList = new ArrayList<>();

        Delivery delivery = Delivery.builder()
                .deliveryInformationList(new ArrayList<>())
                .order(order)
                .deliveryInformationList(deliveryInformationList)
                .user(order.getUser())
                .build();

        deliveryRepository.save(delivery);
    }

    @Transactional
    public void updateDeliveryInTransaction(Long orderId, List<DeliveryCoResponse> responses) {
        Optional<Delivery> delivery = deliveryRepository.findByOrderIdForUpdate(orderId);
        if (delivery.isEmpty()) {
            log.warn("Can not find Delivery by order {}", orderId);
            throw new BusinessException("DELIVERY_NOT_FOUND", "Can not find Delivery by order", HttpStatus.BAD_REQUEST);
        }
        List<Delivery.DeliveryInformation> deliveryInformationList = updateDelivery(responses);

        delivery.get().setDeliveryInformationList(deliveryInformationList);
        deliveryRepository.save(delivery.get());
    }

    @Transactional
    public void updateDeliveryStatusInTransaction(DeliveryResponseMsg msg) {
        long oid = parseOrderId(msg.getOrderId());

        Delivery delivery = deliveryRepository.findByOrderIdForUpdate(oid)
                .orElseThrow(() -> new BusinessException("DELIVERY_NOT_FOUND", "Can not find Delivery by order", HttpStatus.BAD_REQUEST));

        // update the delivery info
        List<Delivery.DeliveryInformation> infos = delivery.getDeliveryInformationList();
        // if info is null use response data to insert
        if (infos == null) {
            infos = new ArrayList<>();
            delivery.setDeliveryInformationList(infos);
        }
        Map<String, Delivery.DeliveryInformation> byId = infos.stream()
                .filter(i -> i.getId() != null)
                .collect(Collectors.toMap(Delivery.DeliveryInformation::getId, Function.identity(), (a, b)->a));

        for (DeliveryResponseMsg.Item in : msg.getUpdates()) {
            String id = in.getDeliveryId();
            Delivery.DeliveryInformation cur = byId.get(id);
            // if not the delivery data add it
            if (cur == null) {
                infos.add(newInfo(in));
                byId.put(id, infos.get(infos.size() - 1));
            } else {
                // according to the
                if (isAfter(in.getStatusUpdatedAt(), cur.getStatusUpdatedAt())) {
                    cur.setStatus(DeliveryStatus.valueOf(in.getStatus()));
                    cur.setType(DeliveryType.valueOf(in.getType()));
                    cur.setStatusUpdatedAt(in.getStatusUpdatedAt());
                    cur.setFromAddress(in.getFromAddress());
                    cur.setToAddress(in.getToAddress());
                    cur.setDetails(in.getDetails());
                }
            }
        }

        deliveryRepository.save(delivery);


        // update the order's status
        var toCustomerLatest = msg.getUpdates().stream()
                .filter(i -> DeliveryType.valueOf(i.getType()) == DeliveryType.TO_CUSTOMER)
                .max(Comparator.comparing(DeliveryResponseMsg.Item::getStatusUpdatedAt));

        OrderStatus newStatus = null;

        if (toCustomerLatest.isPresent()) {
            DeliveryStatus s = DeliveryStatus.valueOf(toCustomerLatest.get().getStatus());
            newStatus = switch (s) {
                case PLACED -> OrderStatus.PICKED_UP;
                case DELIVERING -> OrderStatus.IN_TRANSIT;
                case DELIVERED -> OrderStatus.DELIVERED;
                case LOST -> OrderStatus.DELIVERY_LOST;
                default -> null;
            };
        } else {
            boolean anyLost = msg.getUpdates().stream()
                    .anyMatch(i -> DeliveryStatus.valueOf(i.getStatus()) == DeliveryStatus.LOST);
            if (anyLost) {
                log.info("Some sub package lost on Pick up process");
                newStatus = OrderStatus.DELIVERY_LOST;
            }
        }

        if (newStatus != null) {
            Order order = orderRepository.findByIdForUpdate(oid).orElseThrow();
            if (order.getStatus() != newStatus) {
                // send email to user
                log.info("The oder's status update");
                emailSendProducer.sendEmailByStatus(String.valueOf(order.getId()), newStatus, "The order's delivery status has updated from " + order.getStatus() +" to" + newStatus);
                order.setStatus(newStatus);
                orderRepository.save(order);
            }
        }
    }

    private Delivery.DeliveryInformation toInfo(DeliveryResponseMsg.Item i) {
        return Delivery.DeliveryInformation.builder()
                .id(i.getDeliveryId())
                .status(DeliveryStatus.valueOf(i.getStatus()))
                .type(DeliveryType.valueOf(i.getType()))
                .statusUpdatedAt(i.getStatusUpdatedAt())
                .fromAddress(i.getFromAddress())
                .toAddress(i.getToAddress())
                .build();
    }

    private long parseOrderId(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            log.error("Order id must be numeric, Can not parse order id {}", s);
            throw new RuntimeException("Order id must be numeric, Can not parse order id " + s);
        }
    }


    private Delivery.DeliveryInformation newInfo(DeliveryResponseMsg.Item i) {
        return Delivery.DeliveryInformation.builder()
                .id(i.getDeliveryId())
                .status(DeliveryStatus.valueOf(i.getStatus()))
                .type(DeliveryType.valueOf(i.getType()))
                .statusUpdatedAt(i.getStatusUpdatedAt())
                .fromAddress(i.getFromAddress())
                .toAddress(i.getToAddress())
                .details(i.getDetails())
                .build();
    }

    private boolean isAfter(Instant a, Instant b) {
        if (a == null) return false;
        if (b == null) return true;
        return a.isAfter(b);
    }

    private List<Delivery.DeliveryInformation> updateDelivery(List<DeliveryCoResponse> responses) {
        List<Delivery.DeliveryInformation> deliveryInformationList = new ArrayList<>();
        for (DeliveryCoResponse response : responses) {
            Delivery.DeliveryInformation deliveryInformation = new Delivery.DeliveryInformation();
            deliveryInformation.setId(response.getId());
            deliveryInformation.setStatusUpdatedAt(response.getStatusUpdatedAt());
            deliveryInformation.setDetails(response.getDetails());
            deliveryInformation.setType(response.getType());
            deliveryInformation.setStatus(response.getStatus());
            deliveryInformation.setToAddress(response.getToAddress());
            deliveryInformation.setFromAddress(response.getFromAddress());
            deliveryInformationList.add(deliveryInformation);
        }
        return deliveryInformationList;
    }
}
