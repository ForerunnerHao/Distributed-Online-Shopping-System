ROOT URI: localhost:8084


Delivery RestAPI: /api/rest/v1/delivery

1. POST api/rest/v1/delivery
Sample POST request:

{
    "detail":"abc",
    "type": "WAREHOUSE_PICKUP",
    "toAddress":"DeliveryCo",
    "fromAddress": "Warehouse",
    "orderId": "1" <------------ The orderId of the order in Store app

}

type:: 2 valid choices "WAREHOUSE_PICKUP" and "TO_CUSTOMER". create all the warehouse pickup deliveries first ("type":"WAREHOUSE_PICKUP"), then create the delivery to customer ("type":"TO_CUSTOMER")
with that choice of type
2. GET api/rest/v1/delivery/{deliveryId}
Get the delivery data of that delivery

3. GET api/rest/v1/delivery/order/{orderId}
Get all deliveries associated with that order

PLACED, //System received delivery details
PREPARING, //Picking up from warehouses address
DELIVERING, //Delivering to the customer address
DELIVERED, //Delivered to the customer
LOST, //Lost
CANCELLED //Customer cancelled