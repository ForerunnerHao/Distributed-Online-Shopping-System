package Tutorial7_8.Store.service;

import Tutorial7_8.Common.enums.OrderStatus;
import Tutorial7_8.Common.enums.PaymentStatus;
import Tutorial7_8.Store.dto.order.OrderCreateRequest;
import Tutorial7_8.Store.dto.order.OrderDTO;
import Tutorial7_8.Store.dto.payment.PaymentDTO;
import Tutorial7_8.Store.dto.payment.RefundRequest;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Item;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.model.User;
import Tutorial7_8.Store.repository.ItemRepository;
import Tutorial7_8.Store.repository.OrderRepository;

import Tutorial7_8.Store.repository.UserRepository;
import Tutorial7_8.Store.service.Email.EmailService;
import Tutorial7_8.Store.service.Email.delaySend.EmailSendProducer;
import Tutorial7_8.Store.service.payment.PaymentService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Service
@Slf4j
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    private final EmailSendProducer emailSendProducer;

    public OrderDTO createOrder(String userId, OrderCreateRequest request) {
        // get user
        User user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new BusinessException("NOT_FOUND_USER", "User not found", HttpStatus.NOT_FOUND));

        // get item
        Item item = itemRepository.findById(request.getItem_id())
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ITEM", "Item not found", HttpStatus.NOT_FOUND));

        // create order
        Order order = Order.builder()
                .user(user)
                .item(item)
                .expiresAt(Instant.now().plusSeconds(900))
                .quantity(request.getQuantity())
                .build();

        orderRepository.saveAndFlush(order);
        BigDecimal totalAmount = item.getPrice().multiply(new BigDecimal(request.getQuantity()));

        // reserve the item on the warehouse
        return OrderDTO.builder()
                .id(order.getId())
                .item_id(item.getId())
                .user_id(user.getId())
                .status(order.getStatus())
                .quantity(order.getQuantity())
                .total_amount(totalAmount)
                .warehouse_reservations(order.getWarehouseReservations())
                .created_at(order.getCreatedAt())
                .expires_at(order.getExpiresAt())
                .updated_at(order.getUpdatedAt())
                .build();
    }

    public OrderDTO getOneOrderById(String orderId) {
        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ORDER", "Order not found", HttpStatus.NOT_FOUND));
        return OrderDTO.builder()
                .id(order.getId())
                .user_id(order.getUser().getId())
                .status(order.getStatus())
                .warehouse_reservations(order.getWarehouseReservations())
                .quantity(order.getQuantity())
                .item_id(order.getItem().getId())
                .expires_at(order.getExpiresAt())
                .updated_at(order.getUpdatedAt())
                .build();
    }

    public List<OrderDTO> getAllOrders(String userId) {
        List<Order> orders = orderRepository.findAllByUser_Id(Long.valueOf(userId));
        List<OrderDTO> list = new ArrayList<>();
        for (Order order : orders) {
            OrderDTO orderDTO = OrderDTO.builder()
                    .id(order.getId())
                    .user_id(order.getUser().getId())
                    .status(order.getStatus())
                    .quantity(order.getQuantity())
                    .item_id(order.getItem().getId())
                    .expires_at(order.getExpiresAt())
                    .updated_at(order.getUpdatedAt())
                    .build();
            list.add(orderDTO);
        }
        return list;
    }

    @Transactional
    public OrderDTO cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdForUpdate(Long.valueOf(orderId))
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ORDER", "Order not found", HttpStatus.NOT_FOUND));

        OrderStatus status = order.getStatus();

        if (isTerminal(status) || hasShippingStarted(status)) {
            throw new BusinessException("CANNOT_CANCEL",
                    "Order cannot be canceled in status: " + status, HttpStatus.CONFLICT);
        }

        if (status == OrderStatus.PAID) {

            order.setStatus(OrderStatus.REFUND_PENDING);
            orderRepository.save(order);

            try {
                RefundRequest req = new RefundRequest();
                req.setOrderId(Long.valueOf(orderId));
                req.setReason("User cancel the order");

                PaymentDTO p = paymentService.refund(req, userId);

                if (p != null && PaymentStatus.REFUNDED.name().equals(p.getPaymentStatus())) {
                    order.setStatus(OrderStatus.REFUNDED);
                    orderRepository.save(order);

                    emailSendProducer.sendEmailByStatus(orderId, OrderStatus.CANCELLED, "the order is canceled by user");
                } else {
                    log.info("Refund pending for order {}", orderId);
                }
            } catch (Exception e) {
                log.warn("Refund request failed, keep REFUND_PENDING. order={}, err={}", orderId, e.getMessage());
            }
        } else {
            if (status == OrderStatus.RESERVED) {
                boolean released = inventoryService.releaseItemForOrder(order.getId());
                if (!released) {
                    log.warn("Release stock failed for order {}", orderId);
                }
            }
            order.setStatus(OrderStatus.CANCELED_BY_USER);
            orderRepository.save(order);
            emailSendProducer.sendEmailByStatus(orderId, OrderStatus.CANCELLED, "The order canceled by user");
        }
        return toDTO(order);
    }


    private boolean hasShippingStarted(OrderStatus s) {
        return s == OrderStatus.SHIPMENT_REQUESTED
                || s == OrderStatus.SHIPMENT_ACCEPTED
                || s == OrderStatus.PICKED_UP
                || s == OrderStatus.IN_TRANSIT
                || s == OrderStatus.DELIVERED
                || s == OrderStatus.DELIVERY_LOST;
    }

    private boolean isTerminal(OrderStatus s) {
        return s == OrderStatus.CANCELLED
                || s == OrderStatus.CANCELED_BY_USER
                || s == OrderStatus.CANCELED_BY_SYSTEM
                || s == OrderStatus.CANCELED_TIMEOUT
                || s == OrderStatus.REFUNDED
                || s == OrderStatus.PAYMENT_PENDING
                || s == OrderStatus.REFUND_PENDING;
    }

    private long parseOrderId(String orderId) {
        try {
            return Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            throw new BusinessException("BAD_ORDER_ID", "Order id must be numeric", HttpStatus.BAD_REQUEST);
        }
    }

    private OrderDTO toDTO(Order order) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        try {
            totalAmount = BigDecimal.valueOf(order.getQuantity()).multiply(order.getItem().getPrice());
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        }
        return OrderDTO.builder()
                .id(order.getId())
                .user_id(order.getUser().getId())
                .status(order.getStatus())
                .quantity(order.getQuantity())
                .warehouse_reservations(order.getWarehouseReservations())
                .item_id(order.getItem().getId())
                .expires_at(order.getExpiresAt())
                .updated_at(order.getUpdatedAt())
                .created_at(order.getCreatedAt())
                .item_sku(order.getItem().getSku())
                .total_amount(totalAmount)
                .build();
    }
}
