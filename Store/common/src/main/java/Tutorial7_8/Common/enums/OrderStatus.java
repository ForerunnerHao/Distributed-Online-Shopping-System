package Tutorial7_8.Common.enums;

public enum OrderStatus {
    CREATED,
    RESERVED,   // system will reserve the order items in warehouse
    OUT_OF_STOCK,
    PAYMENT_PENDING,
    REFUND_PENDING,
    REFUNDED,
    PAID,
    SHIPMENT_REQUESTED,
    SHIPMENT_ACCEPTED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    DELIVERY_LOST,
    CANCELLED,
    CANCELED_TIMEOUT,
    CANCELED_BY_USER,
    CANCELED_BY_SYSTEM,
    PAYMENT_FAILED,
    FAILED,
    WAREHOUSE_FAILED,
}
