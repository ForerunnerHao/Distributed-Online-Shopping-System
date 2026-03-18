package com.delivery.DeliveryCo.model.enums;

/**Enum for delivery status, has 6 standard elements */
public enum DeliveryStatus{
    PLACED, //System received delivery details
    PREPARING, //Picking up from warehouses address
    DELIVERING, //Delivering to the customer address
    DELIVERED, //Delivered to the customer
    LOST, //Lost
    CANCELLED //Customer cancelled
}