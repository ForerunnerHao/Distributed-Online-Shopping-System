package Tutorial7_8.Store.service.delivery.delaySend;

import Tutorial7_8.Common.enums.OrderStatus;
import Tutorial7_8.Store.dto.delivery.DeliveryCoResponse;
import Tutorial7_8.Store.service.Email.delaySend.EmailSendProducer;
import Tutorial7_8.Store.service.delivery.DeliveryTxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static Tutorial7_8.Store.service.delivery.delaySend.DeliveryDelaySendAmqpConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryDelayConsumer {

    private final ObjectMapper mapper;
    private final DeliveryTxService deliveryTxService;

    private final DeliveryDelayProducer producer;
    private final EmailSendProducer emailSendProducer;

    private static final String DELIVERY_URL = "http://localhost:8084/api/rest/v1/delivery/batch";

    private final int deliveryTimeoutMs = 3000;

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(deliveryTimeoutMs))
                .build();
    }

    @RabbitListener(queues = DD_MAIN)
    public void sendDeliveryRequest(DeliveryRequestMsg msg) {
        log.info("Delivery request queue message received, starting send delivery request");
        Long orderId = Long.valueOf(msg.deliveryCoRequests.get(0).getOrderId());

        try {
            // check_order_status
            log.info("check_order_status");
            if(!checkOrderStatus(orderId)){
                return;
            }

            // create delivery
            log.info("create delivery");
            deliveryTxService.createNewDeliveryInTransaction(orderId);
        }
        catch (Exception e) {
            log.error(e.getMessage());
            producer.sendFail(msg);
            return;
        }

        if (orderId == 114514) {
            log.info("[TEST: 10s delay msg] Delivery request queue message received, sending delivery");
            deliveryTxService.updateOrderStatusInTransaction(orderId, OrderStatus.SHIPMENT_REQUESTED);
            try {
                List<DeliveryCoResponse> responses = sendDeliveryRequestToExternalService(msg);
                log.info(responses.toString());

                // update the delivery info
                deliveryTxService.updateDeliveryInTransaction(orderId, responses);
                deliveryTxService.updateOrderStatusInTransaction(orderId, OrderStatus.SHIPMENT_REQUESTED);
            } catch (Exception e) {
                log.error(e.getMessage());
                producer.sendFail(msg);
            }
            return;
        }


        deliveryTxService.updateOrderStatusInTransaction(orderId, OrderStatus.SHIPMENT_REQUESTED);
        try {
            log.info("sendDeliveryRequestToExternalService");
            List<DeliveryCoResponse> responses = sendDeliveryRequestToExternalService(msg);
            deliveryTxService.handleSuccessResponseInTransaction(orderId, responses);
        } catch (Exception e) {
            log.error(e.getMessage());
            deliveryTxService.handleFailureInTransaction(orderId, e);
            producer.sendFail(msg);
        }
    }


    public List<DeliveryCoResponse> sendDeliveryRequestToExternalService(DeliveryRequestMsg msg) throws Exception {
        String jsonBody = mapper.writeValueAsString(msg.getDeliveryCoRequests());
        log.info("Send delivery request to delivery company, orderId {}",
                msg.getDeliveryCoRequests().get(0).getOrderId());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DELIVERY_URL))
                .timeout(Duration.ofMillis(deliveryTimeoutMs))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> resp = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("DeliveryCo Response HTTP non-2xx: " + resp.statusCode());
        }
        DeliveryCoResponse[] arr = mapper.readValue(resp.body(), DeliveryCoResponse[].class);
        return java.util.Arrays.asList(arr);
    }

    public boolean checkOrderStatus(Long orderId) {
        OrderStatus orderStatus = deliveryTxService.getOderStatusInTransaction(orderId);

        if (orderStatus != OrderStatus.PAID) {
            log.warn("Delivery request queue message received order status is not PAID");
            if (orderStatus == OrderStatus.CANCELED_BY_USER) {
                log.warn("Delivery request queue message received order status is cancelled");
            }
            return false;
        }
        return true;
    }
}
