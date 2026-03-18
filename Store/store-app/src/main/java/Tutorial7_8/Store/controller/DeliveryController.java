package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.delivery.DeliveryDTO;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Delivery;
import Tutorial7_8.Store.service.delivery.DeliveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery", description = "Relative delivery API endpoints")
public class DeliveryController {
    DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<List<DeliveryDTO>> getAll(HttpServletRequest request) {
        log.info("get all deliveries info /demo");
        String userId = (String) request.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        List<DeliveryDTO> deliveries = deliveryService.getAllDeliveriesByUserId(userId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("{delivery_id}")
    public ResponseEntity<DeliveryDTO> getById(@PathVariable("delivery_id") String delivery_id, HttpServletRequest request) {
        log.info("get delivery by id /demo");
        String userId = (String) request.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }

        DeliveryDTO delivery = deliveryService.getDeliveryById(delivery_id);

        return ResponseEntity.ok(delivery);
    }
}
