package Tutorial7_8.Store.service.delivery;

import Tutorial7_8.Common.enums.DeliveryType;
import Tutorial7_8.Store.dto.delivery.DeliveryCoRequest;
import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import Tutorial7_8.Store.dto.delivery.DeliveryDTO;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.model.Delivery;
import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.repository.DeliveryRepository;
import Tutorial7_8.Store.service.delivery.delaySend.DeliveryDelayProducer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class DeliveryService {

    private final DeliveryDelayProducer producer;
    private final DeliveryRepository deliveryRepository;

    public void sendRequestToDeliverCo(List<DeliveryCoRequest> deliveryCoRequests){
        // TODO send the deliveries to 10s delay queue then send it to DeliveryCo
        log.info("Sending delivery co requests");
        producer.delayDeliveryRequest(deliveryCoRequests);
    }

    public void deliverItem(List<Order.WarehouseReservation> wrs, Long orderId){
        log.info("Delivering items, parse the");
        List<DeliveryCoRequest> deliveryCoRequests = new ArrayList<>();


        for(int i=0; i<wrs.size(); i++){
            String details = String.format("Total: %s, this is No.%s package", wrs.size(), (i+1));
            DeliveryCoRequest request = DeliveryCoRequest.builder()
                    .type(DeliveryType.WAREHOUSE_PICKUP)
                    .orderId(String.valueOf(orderId))
                    .fromAddress(wrs.get(i).getWarehouseCode() + "_address")
                    .toAddress("1A Delivery Headquarter, DeliveryCo")
                    .detail(details)
                    .build();

            deliveryCoRequests.add(request);
        }

        DeliveryCoRequest finalRequest = DeliveryCoRequest.builder()
                .type(DeliveryType.TO_CUSTOMER)
                .orderId(String.valueOf(orderId))
                .fromAddress("DeliveryCo")
                .toAddress("user_address")
                .detail("When all packages are delivered, it will deliver to user address")
                .build();

        deliveryCoRequests.add(finalRequest);
        sendRequestToDeliverCo(deliveryCoRequests);
    }


    public void processDeliveryResponse(List<DeliveryCoResponse> deliveryCoResponse) {
        int lastIndex = deliveryCoResponse.size() - 1;
        log.info("getDeliveryRespond: deliveryId:{}, deliveryType:{}", deliveryCoResponse.get(lastIndex).getId(), deliveryCoResponse.get(lastIndex).getType());
        // create delivery item in database
        log.info("create delivery item in database");
        // update the order's status
        log.info("update the order's status");
    }

    public void testDelivery() {
        List<DeliveryCoRequest> deliveryCoRequests = new ArrayList<>();

        DeliveryCoRequest re1 = DeliveryCoRequest.builder()
                .detail("test2")
                .toAddress("user_address")
                .fromAddress("warehouse_address")
                .type(DeliveryType.WAREHOUSE_PICKUP)
                .orderId("114514")
                .build();

        DeliveryCoRequest re2 = DeliveryCoRequest.builder()
                .detail("test2")
                .toAddress("user_address")
                .fromAddress("warehouse_address")
                .type(DeliveryType.WAREHOUSE_PICKUP)
                .orderId("114514")
                .build();

        DeliveryCoRequest ref = DeliveryCoRequest.builder()
                .detail("test2")
                .toAddress("user_address")
                .fromAddress("DeliveryCo")
                .type(DeliveryType.TO_CUSTOMER)
                .orderId("114514")
                .build();


        deliveryCoRequests.add(re1);
        deliveryCoRequests.add(re2);
        deliveryCoRequests.add(ref);

        sendRequestToDeliverCo(deliveryCoRequests);
    }

    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveriesByUserId(String userId) {
        long uid = parseUserId(userId);
        List<Delivery> deliveries = deliveryRepository.findAllByUser_Id(uid);
        if (deliveries.isEmpty()) return List.of();

        return deliveries.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliveryDTO getDeliveryById(String deliveryId) {
        long id = parseId(deliveryId);

        Delivery d = deliveryRepository.findById((int) id)
                .orElseThrow(() -> new BusinessException(
                        "DELIVERY_NOT_FOUND", "Delivery not found", HttpStatus.NOT_FOUND));

        return toDto(d);
    }

    private long parseId(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) {
            throw new BusinessException("BAD_ID", "deliveryId must be numeric", HttpStatus.BAD_REQUEST);
        }
    }

    private long parseUserId(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) {
            throw new BusinessException("BAD_USER_ID", "userId must be numeric", HttpStatus.BAD_REQUEST);
        }
    }

    private DeliveryDTO toDto(Delivery d) {
        return DeliveryDTO.builder()
                .id(d.getId())
                .orderId(String.valueOf(d.getOrder().getId()))
                .packages(
                        (d.getDeliveryInformationList() == null ? List.<Delivery.DeliveryInformation>of() : d.getDeliveryInformationList())
                                .stream()
                                .map(this::toPackage)
                                .toList()
                )
                .build();
    }

    private DeliveryDTO.Package toPackage(Delivery.DeliveryInformation i) {
        return DeliveryDTO.Package.builder()
                .deliveryId(i.getId())
                .type(i.getType() == null ? null : i.getType().name())
                .status(i.getStatus() == null ? null : i.getStatus().name())
                .statusUpdatedAt(i.getStatusUpdatedAt())
                .details(i.getDetails())
                .fromAddress(i.getFromAddress())
                .toAddress(i.getToAddress())
                .build();
    }
}
