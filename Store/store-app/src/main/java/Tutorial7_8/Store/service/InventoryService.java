package Tutorial7_8.Store.service;

import Tutorial7_8.Common.enums.ReservationStatus;
import Tutorial7_8.Store.dto.warehouse.InventoryResponse;
import Tutorial7_8.Common.enums.InventoryResponseStatus;
import Tutorial7_8.Store.dto.item.ItemDTO;
import Tutorial7_8.Store.dto.item.ItemWarehouseDTO;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Item;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Common.enums.OrderStatus;
import Tutorial7_8.Store.repository.ItemRepository;
import Tutorial7_8.Store.repository.OrderRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tutorial7_8.proto.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class InventoryService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @GrpcClient("warehouse1")
    WarehouseServiceGrpc.WarehouseServiceBlockingStub wh1Stub;

    @GrpcClient("warehouse2")
    WarehouseServiceGrpc.WarehouseServiceBlockingStub wh2Stub;

    @Autowired
    public InventoryService(OrderRepository orderRepository, ItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    public ItemDTO getItemStockById(Long itemId) {
        List<WarehouseServiceGrpc.WarehouseServiceBlockingStub> warehouses = List.of(wh1Stub, wh2Stub);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ITEM", "Item not found by item_id", HttpStatus.NOT_FOUND));

        ItemDTO itemDTO = ItemDTO.builder()
                .sku(item.getSku())
                .name(item.getName())
                .id(item.getId())
                .price(item.getPrice())
                .itemWarehouseList(new ArrayList<>())
                .build();

        for (int i = 0; i < warehouses.size(); i++) {
            var stub = warehouses.get(i);
            StockResponse stockResponse = stub.stock(StockRequest.newBuilder().setSku(item.getSku()).build());

            if (!stockResponse.getOk()) {
                log.warn("warehouse-{}, failed response, try next", (i + 1));
                continue;
            }
            ItemWarehouseDTO itemWarehouseDTO = ItemWarehouseDTO.builder()
                    .deductedQty(stockResponse.getDeductedQty())
                    .totalQty(stockResponse.getTotalQty())
                    .reservedQty(stockResponse.getReservedQty())
                    .warehouseCode("WH-" + (i + 1))
                    .warehouseName("warehouse-" + (i + 1))
                    .build();
            itemDTO.getItemWarehouseList().add(itemWarehouseDTO);
        }
        return itemDTO;
    }

    public List<ItemDTO> getAllItems() {
        List<Item> items = itemRepository.findAll();
        List<ItemDTO> itemDTOList = new ArrayList<>();
        for (Item item : items) {
            ItemDTO itemDTO = ItemDTO.builder()
                    .sku(item.getSku())
                    .name(item.getName())
                    .id(item.getId())
                    .price(item.getPrice())
                    .itemWarehouseList(null)
                    .build();
            itemDTOList.add(itemDTO);
        }
        return itemDTOList;
    }

    @Transactional
    public InventoryResponse reserveItemForOrder(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ORDER", "Order not found", HttpStatus.NOT_FOUND));
        String itemSku = order.getItem().getSku();
        Long itemId = order.getItem().getId();
        int quantity = order.getQuantity();

        List<WarehouseServiceGrpc.WarehouseServiceBlockingStub> warehouses = List.of(wh1Stub, wh2Stub);
        int remainItemQty = quantity;

        Map<Integer, ReserveResponse> responses = new LinkedHashMap<>();

        InventoryResponse inventoryRes = InventoryResponse.builder()
                .itemSku(itemSku)
                .orderId(orderId)
                .status(InventoryResponseStatus.PENDING)
                .message("Init")
                .build();

        for (int i = 0; i < warehouses.size(); i++) {
            if (remainItemQty == 0) break;

            // 1. generate reservationId
            String reservationId = orderId + "-wh" + (i + 1);

            // 2. build Reserve Request
            ReserveRequest request = ReserveRequest.newBuilder()
                    .setOrderId(String.valueOf(orderId))
                    .setItemId(String.valueOf(itemId))
                    .setSku(itemSku)
                    .setQuantity(remainItemQty)
                    .setReservationId(reservationId)
                    .build();
            // check the timeout
            try {
                var stub = warehouses.get(i).withDeadlineAfter(2000, TimeUnit.MILLISECONDS);
                ReserveResponse response = stub.reserve(request);
                if (!response.getOk()) {
                    log.warn("{}: Reservation response failed", ("wh" + i));
                    inventoryRes.setStatus(InventoryResponseStatus.CONNECTION_TIMEOUT);
                    inventoryRes.setMessage("Reservation response time out");
                    continue;
                }
                if (response.getReservedQty() > 0) {
                    //
                    responses.put(i, response);
                    remainItemQty -= response.getReservedQty();
                    log.info("warehouse: wh-{}, item: {}, reservedQty: {}", (i + 1), itemSku, response.getReservedQty());

                    // jsonb insert the warehouse item's reservation info
                    if (order.getWarehouseReservations() == null) {
                        order.setWarehouseReservations(new ArrayList<>());
                    }
                    Order.WarehouseReservation warehouseReservation = Order.WarehouseReservation.builder()
                            .reservationId(reservationId)
                            .reservedQty(response.getReservedQty())
                            .status(OrderStatus.RESERVED.toString())
                            .warehouseCode("wh-" + (i + 1))
                            .build();

                    order.getWarehouseReservations()
                            .removeIf(s -> Objects.equals(s.getReservationId(), reservationId));
                    order.getWarehouseReservations().add(warehouseReservation);
                    orderRepository.save(order);
                } else {
                    log.info("warehouse: wh-{}, item: {} stock is 0", (i + 1), itemSku);
                }
            } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if (code == Status.Code.DEADLINE_EXCEEDED) {
                    inventoryRes.setStatus(InventoryResponseStatus.CONNECTION_TIMEOUT);
                    inventoryRes.setMessage("Warehouse RPC timed out");
                    log.warn("[wh{}] gRPC timeout (DEADLINE_EXCEEDED): {}", i, e.getMessage());
                } else if (code == Status.Code.UNAVAILABLE) {
                    // The target service is not up/the port is blocked/the connection is reset/NameResolver failed, etc.
                    inventoryRes.setStatus(InventoryResponseStatus.CONNECTION_ERROR);
                    inventoryRes.setMessage("Warehouse service unavailable");
                    log.warn("[wh{}] gRPC unavailable: {}", i, e.getMessage());
                } else if (code == Status.Code.ABORTED || code == Status.Code.FAILED_PRECONDITION) {
                    // Optimistic lock conflict/business preconditions are not met (if the server returns with these statuses)
                    inventoryRes.setStatus(InventoryResponseStatus.CONNECTION_ERROR);
                    inventoryRes.setMessage("Warehouse conflict/precondition failed");
                    log.warn("[wh{}] gRPC business conflict: {}", i, e.getMessage());
                } else {
                    inventoryRes.setStatus(InventoryResponseStatus.WAREHOUSE_FAILED);
                    inventoryRes.setMessage("Warehouse RPC failed");
                    log.warn("[wh{}] gRPC exception ({}): {}", i, code, e.getMessage());
                }
            } catch (Exception e) {
                // Non-gRPC exceptions (serialization, NPE, etc.)
                inventoryRes.setStatus(InventoryResponseStatus.WAREHOUSE_FAILED);
                inventoryRes.setMessage("Unexpected client error");
                log.warn("[wh{}] non-gRPC exception: {}", i, e.toString());
            }
        }

        if (remainItemQty == quantity) {
            order.setStatus(OrderStatus.WAREHOUSE_FAILED);
            orderRepository.saveAndFlush(order);
            log.warn("The all warehouses can not reserve the item, please check the detail log info");
        } else if (remainItemQty == 0) {
            order.setStatus(OrderStatus.RESERVED);
            orderRepository.saveAndFlush(order);
            inventoryRes.setStatus(InventoryResponseStatus.SUCCESS);
            inventoryRes.setMessage("Reservation successful");
            log.info("The item: {} X{} have been reserved", order.getItem().getSku(), order.getQuantity());
        } else {
            // update the order's status
            order.setStatus(OrderStatus.OUT_OF_STOCK);
            orderRepository.save(order);

            inventoryRes.setStatus(InventoryResponseStatus.OUT_OF_STOCK);

            // release the reserved item on warehouse
            for (Map.Entry<Integer, ReserveResponse> entry : responses.entrySet()) {
                ReserveResponse response = entry.getValue();
                Integer index = entry.getKey();

                ReleaseRequest rr = ReleaseRequest.newBuilder()
                        .setReservationId(response.getReservationId())
                        .setReservationStatus(PReservationStatus.RELEASED)
                        .build();

                // send release request to specified warehouse
                try {
                    var stub = warehouses.get(index).withDeadlineAfter(500, TimeUnit.MILLISECONDS);
                    ReleaseResponse releaseRes = stub.release(rr);

                    if (!releaseRes.getOk()) {
                        order.setStatus(OrderStatus.WAREHOUSE_FAILED);
                        inventoryRes.setStatus(InventoryResponseStatus.WAREHOUSE_FAILED);
                        orderRepository.saveAndFlush(order);
                        log.warn("{}: Release response failed, need manual/backend intervention", ("wh" + index));
                    }
                } catch (Exception e) {
                    order.setStatus(OrderStatus.WAREHOUSE_FAILED);
                    inventoryRes.setStatus(InventoryResponseStatus.WAREHOUSE_FAILED);
                    orderRepository.saveAndFlush(order);
                    log.warn("[{}]: Release response exception, need manual/backend intervention: {}", ("wh" + index), e.getMessage());
                }
            }
        }

        return inventoryRes;
    }

    @Transactional
    public boolean releaseItemForOrder(Long orderId) {
        log.info("releaseItemForOrder: {}", orderId);

        List<WarehouseServiceGrpc.WarehouseServiceBlockingStub> warehouses = List.of(wh1Stub, wh2Stub);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ORDER", "Order not found", HttpStatus.NOT_FOUND));
        int orderQty = order.getQuantity();
        int releaseQty = 0;

        List<Order.WarehouseReservation> wrs = order.getWarehouseReservations();
        if (wrs.isEmpty()) {
            log.warn("[{}]: No warehouse reservations available", orderId);
            return false;
        }

        for (int i = 0; i < wrs.size(); i++) {
            String reservationId = wrs.get(i).getReservationId();
            String warehouseId = wrs.get(i).getWarehouseCode();
            int warehouseNumber;

            if (warehouseId.startsWith("wh-") && warehouseId.length() > 3) {
                String numberPart = warehouseId.substring(3);
                warehouseNumber = Integer.parseInt(numberPart) - 1;
                if (warehouseNumber >= warehouses.size() || warehouseNumber < 0) {
                    log.warn("[{}]: Invalid warehouse number： {}", orderId, warehouseId);
                    continue;
                }
            } else {
                log.warn("Can not get WarehouseCode{}", warehouseId);
                continue;
            }
            try {
                ReleaseRequest request = ReleaseRequest.newBuilder()
                        .setReservationId(reservationId)
                        .setReservationStatus(PReservationStatus.RELEASED)
                        .build();
                var stub = warehouses.get(warehouseNumber);
                ReleaseResponse response = stub.release(request);

                if (!response.getOk()) {
                    log.warn("[{}] - reservationId: {} : Release response failed, message: {}", ("wh" + (warehouseNumber +1)), reservationId, response.getMessage());
                    wrs.get(i).setStatus(ReservationStatus.ERROR.toString());
                    continue;
                }
                log.info("[{}] - reservationId: {} release: item: {}, release: {}, total: {}, reserved: {}, deduced: {}",
                        ("wh" + (warehouseNumber +1)),
                        response.getReservationId(),
                        response.getItemSku(),
                        response.getReleasedQty(),
                        response.getTotalQty(),
                        response.getReservedQty(),
                        response.getDeductedQty()
                );
                releaseQty += response.getReleasedQty();

                // update order warehouse stock data
                wrs.get(i).setStatus(ReservationStatus.RELEASED.toString());

            } catch (Exception e) {
                log.warn("[wh{}] release request exception: {}", i, e.toString());
            }
        }

        boolean result = orderQty == releaseQty;
        if (result){
            log.info("All reservation stock have been released, orderId: {}", orderId);
            order.setStatus(OrderStatus.CANCELLED);
            order.setCanceledAt(Instant.now());
        }
        orderRepository.save(order);

        return result;
    }

    @Transactional
    public boolean confirmItemForOrder(Long orderId) {
        log.info("confirmItemForOrder: {}", orderId);
        List<WarehouseServiceGrpc.WarehouseServiceBlockingStub> warehouses = List.of(wh1Stub, wh2Stub);
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND_ORDER", "Order not found", HttpStatus.NOT_FOUND));

        List<Order.WarehouseReservation> wrs = order.getWarehouseReservations();
        if (wrs.isEmpty()) {
            log.warn("[{}]: Confirm Request - No warehouse reservations available", orderId);
            return false;
        }

        int orderQty = order.getQuantity();
        int confirmQty = 0;
        for (int i = 0; i < wrs.size(); i++) {
            String reservationId = wrs.get(i).getReservationId();
            String warehouseId = wrs.get(i).getWarehouseCode();
            int warehouseNumber;

            if (warehouseId.startsWith("wh-") && warehouseId.length() > 3) {
                String numberPart = warehouseId.substring(3);
                warehouseNumber = Integer.parseInt(numberPart) -1;
                if (warehouseNumber >= warehouses.size() || warehouseNumber < 0) {
                    log.warn("Confirm - [{}]: Invalid warehouse number： {}", orderId, warehouseId);
                    continue;
                }
            } else {
                log.warn("Confirm Request - Can not get WarehouseCode{}", warehouseId);
                continue;
            }

            try {
                ConfirmRequest request = ConfirmRequest.newBuilder()
                        .setReservationId(reservationId)
                        .build();
                var stub = warehouses.get(warehouseNumber);
                ConfirmResponse response = stub.confirm(request);

                if (!response.getOk()) {
                    log.warn("[{}] - reservationId: {} : Confirm response failed, message: {}", ("wh" + (warehouseNumber +1)), reservationId, response.getMessage());
                    wrs.get(i).setStatus(ReservationStatus.ERROR.toString());
                    continue;
                }
                log.info("[{}] - reservationId: {} Confirm message: {}: item: {}, confirm: {}, total: {}, reserved: {}, deduced: {}",
                        ("wh" + (warehouseNumber +1)),
                        response.getReservationId(),
                        response.getItemSku(),
                        response.getMessage(),
                        response.getConfirmedQty(),
                        response.getTotalQty(),
                        response.getReservedQty(),
                        response.getDeductedQty()
                );
                confirmQty += response.getConfirmedQty();
                wrs.get(i).setStatus(ReservationStatus.CONFIRMED.toString());
            } catch (Exception e) {
                log.warn("[wh{}] confirm request exception: {}", i, e.toString());
            }
        }
        boolean result = orderQty == confirmQty;
        if (result){
            log.info("Confirm Request - All reservation stock have been released, orderId: {}", orderId);
            order.setStatus(OrderStatus.PAID);
        }

        orderRepository.save(order);

        return result;
    }


}
