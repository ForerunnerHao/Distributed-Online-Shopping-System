package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.order.OrderCancelRequest;
import Tutorial7_8.Store.dto.warehouse.InventoryResponse;
import Tutorial7_8.Store.dto.order.OrderCreateRequest;
import Tutorial7_8.Store.dto.order.OrderDTO;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.service.InventoryService;
import Tutorial7_8.Store.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Relative order API endpoints")
public class OrderController {
    private final OrderService orderService;
    private final InventoryService inventoryService;

    @Autowired
    public OrderController(OrderService orderService, InventoryService inventoryService) {
        this.orderService = orderService;
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "Create order", description = "create a new order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Create successfully",
                    content = @Content(schema = @Schema(implementation = OrderDTO.class))),
            @ApiResponse(responseCode = "400", description = "parameter error")
    })
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderCreateRequest request, HttpServletRequest req) {
        log.info("createOrder /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        // create order (init a new order)
        OrderDTO order = orderService.createOrder(userId, request);

        // reserve item on warehouse
        try {
            InventoryResponse response = inventoryService.reserveItemForOrder(order.getId());
            return switch (response.getStatus()) {
                case OUT_OF_STOCK ->
                        throw new BusinessException("ORDER_OUT_OF_STOCK", "Cannot create order, out of stock", HttpStatus.BAD_REQUEST);
                case WAREHOUSE_FAILED ->
                        throw new BusinessException("WAREHOUSE_FAILED", "Cannot create order, warehouse internal error", HttpStatus.BAD_REQUEST);
                case CONNECTION_TIMEOUT ->
                        throw new BusinessException("CONNECTION_TIMEOUT", "Cannot create order, warehouse connection timeout", HttpStatus.BAD_REQUEST);
                case CONNECTION_ERROR ->
                        throw new BusinessException("CONNECTION_ERROR", "Cannot create order, cannot connect warehouse(timeout or error)", HttpStatus.BAD_REQUEST);
                case SUCCESS -> ResponseEntity.ok(order);
                default -> throw new BusinessException("UNKNOWN_ERROR", "Cannot create order", HttpStatus.BAD_REQUEST);
            };
        } catch (Exception e) {
            throw new BusinessException("ORDER_CREATE_ERROR", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("{order_id}")
    public ResponseEntity<OrderDTO> getOneOrder(HttpServletRequest req,
                                                @PathVariable("order_id") String orderId) {
        log.info("getOneOrder /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        try {
            OrderDTO order = orderService.getOneOrderById(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            throw new BusinessException("ORDER_GET_ERROR", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getAllOrders(HttpServletRequest req) {
        log.info("getAllOrders /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        try {
            List<OrderDTO> orders = orderService.getAllOrders(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            throw new BusinessException("ORDER_GET_ERROR", e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("cancel")
    public ResponseEntity<OrderDTO> cancelOrder(HttpServletRequest req, @RequestBody OrderCancelRequest request) {
        log.info("cancelOrder /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        OrderDTO order = orderService.cancelOrder(request.getOrderId(), userId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("test/release/{order_id}")
    public ResponseEntity<OrderDTO> testReleaseWarehouseStock(HttpServletRequest req,
                                                              @PathVariable("order_id") String orderId){
        log.info("testReleaseWarehouseStock /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }

        boolean ok = inventoryService.releaseItemForOrder(Long.valueOf(orderId));

        if (ok) {
            return ResponseEntity.ok(orderService.getOneOrderById(orderId));
        }else {
            throw new BusinessException("ORDER_RELEASE_ERROR", "Please connect with admin", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("test/confirm/{order_id}")
    public ResponseEntity<OrderDTO> testConfirmWarehouseStock(HttpServletRequest req,
                                                              @PathVariable("order_id") String orderId){
        log.info("testConfirmWarehouseStock /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }

        boolean result = inventoryService.confirmItemForOrder(Long.valueOf(orderId));

        if (result) {
            return ResponseEntity.ok(orderService.getOneOrderById(orderId));
        }else {
            throw new BusinessException("ORDER_CONFIRM_ERROR", "Please connect with admin", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }


}
