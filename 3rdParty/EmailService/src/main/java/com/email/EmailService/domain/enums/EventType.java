package com.email.EmailService.domain.enums;

/**
 * Event types for email notifications
 */
public enum EventType {
    DELIVERY_PICKED_UP,
    DELIVERY_ON_TRUCK,
    DELIVERED,
    ORDER_CANCELLED,
    ORDER_FAILED,
    REFUND_INITIATED,
    REFUND_COMPLETED
}

