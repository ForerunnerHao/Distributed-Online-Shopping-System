package Tutorial7_8.warehouse.rpc;


import Tutorial7_8.warehouse.model.*;
import Tutorial7_8.Common.enums.ReservationStatus;
import Tutorial7_8.warehouse.repository.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import tutorial7_8.proto.*;
import tutorial7_8.proto.WarehouseServiceGrpc;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@GrpcService
public class WarehouseService extends WarehouseServiceGrpc.WarehouseServiceImplBase {

    private final ReservationRepository reservationRepository;
    private final WarehouseStockRepository stockRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    public WarehouseService(
            ReservationRepository reservationRepository,
            WarehouseStockRepository stockRepository, WarehouseStockRepository warehouseStockRepository) {
        this.reservationRepository = reservationRepository;
        this.stockRepository = stockRepository;
        this.warehouseStockRepository = warehouseStockRepository;
    }

    @Override
    @Transactional
    public void reserve(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver) {
        String reservationId = request.getReservationId();
        String orderId = request.getOrderId();
        String itemSku = request.getSku();
        int quantity = request.getQuantity();

        log.info("Reserve request received, the getReservationId: {}, ItemSku: {}, quantity: {}", reservationId, itemSku, quantity);


        Optional<Reservation> existing = reservationRepository.findByReservationId(reservationId);
        if (existing.isPresent()) {
            Reservation r = existing.get();
            responseObserver.onNext(ReserveResponse.newBuilder()
                    .setOk(true)
                    .setReservedQty(r.getQty())
                    .setReservationId(r.getReservationId())
                    .setMessage("Idempotent hit")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // 3. get stock from item id by pessimistic lock
        WarehouseStock stock = stockRepository.findByItemSkuWithLock(itemSku)
                .orElseThrow(() -> new RuntimeException("Stock not found for item: " + itemSku));
        // 4. calculate the reservation quantity
        int reservedQty = Math.min(quantity, stock.getAvailableQty());

        if (reservedQty == 0) log.info("Item: {} stock is (0)empty", itemSku);

        if (reservedQty > 0) {
            // 5. create Reservation
            Reservation reservation = Reservation.builder()
                    .reservationId(reservationId)
                    .orderId(orderId)
                    .itemSku(itemSku)
                    .qty(reservedQty)
                    .status(ReservationStatus.PENDING)
                    .expiresAt(Instant.now().plusSeconds(900)) // 15 minutes
                    .build();
            reservationRepository.save(reservation);

            // 6. update the warehouse stock
            stock.setReservedQty(stock.getReservedQty() + reservedQty);
            stockRepository.saveAndFlush(stock);
            // check
            if (!stock.assertInvariants()) {
                throw new RuntimeException("The reservedQty over Stock");
            }
            log.info("the stock reserve {} item(s), and left {}", reservedQty, stock.getAvailableQty());
        }

        // 7. return response
        ReserveResponse response = ReserveResponse.newBuilder()
                .setOk(true)
                .setReservedQty(reservedQty)
                .setReservationId(reservationId)
                .setMessage(reservedQty == quantity ? "Full reserved" : "Partially reserved")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional
    public void release(ReleaseRequest request, StreamObserver<ReleaseResponse> responseObserver) {
        String reservationId = request.getReservationId();
        log.info("Release request received, the getReservationId: {}", reservationId);
        try {
            Optional<Reservation> opt = reservationRepository.findByReservationId(reservationId);
            if (opt.isEmpty()) {
                log.warn("Reservation not found: {}", reservationId);
                responseObserver.onNext(ReleaseResponse.newBuilder()
                        .setOk(false)
                        .setReservationId(reservationId)
                        .setItemSku("")
                        .setReservedQty(0)
                        .setMessage("Reservation not found: " + reservationId)
                        .build());
                responseObserver.onCompleted();
                return;
            }
            Reservation reservation = opt.get();

            if (reservation.getStatus() != ReservationStatus.PENDING) {
                // if status no PENDING which means completed, user can cancel the order
                log.info("Idempotent hit for {} (status={})", reservationId, reservation.getStatus());

                if (reservation.getStatus() == ReservationStatus.CONFIRMED){

                    log.info("Refund deduced: {}", reservationId);
                    // refund
                    WarehouseStock ws = stockRepository.findByItemSkuWithLock(reservation.getItemSku())
                            .orElse(null);
                    ReleaseResponse.Builder b = ReleaseResponse.newBuilder()
                            .setOk(false)
                            .setReservationId(reservationId)
                            .setMessage("Refund release the deduced stock");
                    if (ws != null) {
                        reservation.setStatus(ReservationStatus.RELEASED);
                        ws.setDeductedQty(ws.getDeductedQty() - reservation.getQty());
                        warehouseStockRepository.saveAndFlush(ws);

                        b.setItemSku(ws.getItemSku())
                                .setOk(true)
                                .setTotalQty(ws.getTotalQty())
                                .setDeductedQty(ws.getDeductedQty())
                                .setReservedQty(ws.getReservedQty())
                                .setReleasedQty(reservation.getQty());
                    }
                    responseObserver.onNext(b.build());
                    responseObserver.onCompleted();
                    return;
                }

                WarehouseStock ws = stockRepository.findByItemSku(reservation.getItemSku())
                        .orElse(null);
                ReleaseResponse.Builder b = ReleaseResponse.newBuilder()
                        .setOk(true)
                        .setReservationId(reservationId)
                        .setMessage("Idempotent hit");
                if (ws != null) {
                    b.setItemSku(ws.getItemSku())
                            .setTotalQty(ws.getTotalQty())
                            .setDeductedQty(ws.getDeductedQty())
                            .setReservedQty(ws.getReservedQty())
                            .setReleasedQty(reservation.getQty());
                }
                responseObserver.onNext(b.build());
                responseObserver.onCompleted();
                return;
            }

            // release reservation
            PReservationStatus reqStatus = request.getReservationStatus();
            ReservationStatus target;
            if (reqStatus == PReservationStatus.EXPIRED) {
                target = ReservationStatus.EXPIRED;
            } else {
                target = ReservationStatus.RELEASED;
            }

            WarehouseStock stock = stockRepository.findByItemSkuWithLock(reservation.getItemSku())
                    .orElse(null);
            if (stock == null) {
                log.warn("Stock not found for sku={} on release {}", reservation.getItemSku(), reservationId);
                responseObserver.onNext(ReleaseResponse.newBuilder()
                        .setOk(false)
                        .setReservationId(reservationId)
                        .setMessage("Stock not found for sku=" + reservation.getItemSku())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // update the warehouse stock quantity
            int reservationQty = reservation.getQty();
            int beforeReserved = stock.getReservedQty();
            int apply = Math.min(reservationQty, Math.max(beforeReserved, 0));
            stock.setReservedQty(beforeReserved - apply);
            stockRepository.saveAndFlush(stock);
            log.info("Release applied: reservationId={}, sku={}, apply={}, reserved {} -> {}",
                    reservationId, stock.getItemSku(), apply, beforeReserved, stock.getReservedQty());

            // update the reservation status
            reservation.setStatus(target);
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);

            // build release response
            ReleaseResponse resp = ReleaseResponse.newBuilder()
                    .setOk(true)
                    .setReservationId(reservationId)
                    .setItemSku(stock.getItemSku())
                    .setTotalQty(stock.getTotalQty())
                    .setDeductedQty(stock.getDeductedQty())
                    .setReservedQty(stock.getReservedQty())
                    .setReleasedQty(apply)
                    .setMessage(apply == reservationQty ? "released" : "released-partial (concurrent adjust)")
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Release failed for {}: {}", request.getReservationId(), e.toString());
            responseObserver.onNext(ReleaseResponse.newBuilder()
                    .setOk(false)
                    .setReservationId(request.getReservationId())
                    .setMessage("Release exception: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    @Transactional
    public void confirm(ConfirmRequest request, StreamObserver<ConfirmResponse> responseObserver) {
        String reservationId = request.getReservationId();
        try {
            Optional<Reservation> opt = reservationRepository.findByReservationId(reservationId);
            if (opt.isEmpty()) {
                log.warn("Confirm request cannot find reservation: {}", reservationId);
                responseObserver.onNext(ConfirmResponse.newBuilder()
                        .setOk(false)
                        .setReservationId(request.getReservationId())
                        .setMessage("Confirm request cannot find reservation:" + reservationId)
                        .setConfirmedQty(0)
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Reservation reservation = opt.get();
            // if reservation status is CONFIRMED, return right now
            if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
                // if status no PENDING which means completed
                log.info("Confirm idempotent hit for {} (status={})", reservationId, reservation.getStatus());

                WarehouseStock ws = stockRepository.findByItemSku(reservation.getItemSku())
                        .orElse(null);
                ConfirmResponse.Builder b = ConfirmResponse.newBuilder()
                        .setOk(true)
                        .setReservationId(reservationId)
                        .setConfirmedQty(reservation.getQty())
                        .setMessage("Confirm idempotent hit");
                if (ws != null) {
                    b.setItemSku(ws.getItemSku())
                            .setTotalQty(ws.getTotalQty())
                            .setDeductedQty(ws.getDeductedQty())
                            .setReservedQty(ws.getReservedQty());
                }
                responseObserver.onNext(b.build());
                responseObserver.onCompleted();
                return;
            }

            // 0. before change the reservation status, its status must be PENDING
            if (reservation.getStatus() != ReservationStatus.PENDING) {
                WarehouseStock snap = stockRepository.findByItemSku(reservation.getItemSku()).orElse(null);
                ConfirmResponse.Builder b = ConfirmResponse.newBuilder()
                        .setOk(false)
                        .setReservationId(reservationId)
                        .setMessage("Cannot confirm non-pending reservation: " + reservation.getStatus());
                if (snap != null) {
                    b.setItemSku(snap.getItemSku())
                            .setTotalQty(snap.getTotalQty())
                            .setDeductedQty(snap.getDeductedQty())
                            .setReservedQty(snap.getReservedQty())
                            .setConfirmedQty(0);
                }
                responseObserver.onNext(b.build());
                responseObserver.onCompleted();
                return;
            }

            // get warehouse data
            WarehouseStock stock = stockRepository.findByItemSkuWithLock(reservation.getItemSku()).orElse(null);
            if (stock == null) {
                log.warn("Stock not found on confirm: sku={}, reservation={}", reservation.getItemSku(), reservationId);
                responseObserver.onNext(ConfirmResponse.newBuilder()
                        .setOk(false)
                        .setReservationId(reservationId)
                        .setMessage("Stock not found for sku=" + reservation.getItemSku())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // update warehouse stock info
            int reservationQty = reservation.getQty();
            int beforeReserved = stock.getReservedQty();
            int apply = Math.min(reservationQty, Math.max(beforeReserved, 0));

            stock.setReservedQty(beforeReserved - apply);
            stock.setDeductedQty(stock.getDeductedQty() + apply);
            stockRepository.saveAndFlush(stock);

            log.info("Confirm applied: reservationId={}, sku={}, apply={}, reserved {} -> {}, deducted -> {}",
                    reservationId, stock.getItemSku(), apply, beforeReserved,
                    stock.getReservedQty(), stock.getDeductedQty());

            // update Reservation status
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setUpdatedAt(Instant.now());
            reservationRepository.save(reservation);

            ConfirmResponse resp = ConfirmResponse.newBuilder()
                    .setOk(true)
                    .setReservationId(reservationId)
                    .setItemSku(stock.getItemSku())
                    .setTotalQty(stock.getTotalQty())
                    .setDeductedQty(stock.getDeductedQty())
                    .setReservedQty(stock.getReservedQty())
                    .setConfirmedQty(apply)
                    .setMessage(apply == reservationQty ? "confirmed" : "confirmed-partial (clamped)")
                    .build();

            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Confirm failed for {}: {}", reservationId, e.toString());
            responseObserver.onNext(ConfirmResponse.newBuilder()
                    .setOk(false)
                    .setReservationId(reservationId)
                    .setMessage("Confirm exception: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }

    }

    @Override
    @Transactional
    public void stock(StockRequest request, StreamObserver<StockResponse> responseObserver) {
        String itemSku = request.getSku();
        WarehouseStock ws = warehouseStockRepository.findByItemSkuWithLock(itemSku)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + itemSku));
        int total = ws.getTotalQty();
        int reserved = ws.getReservedQty();
        int deduced = ws.getDeductedQty();

        log.info("the item: {}, totalQty: {}, reservedQty: {}, deducedQty: {}", itemSku, total, reserved, deduced);

        responseObserver.onNext(StockResponse.newBuilder()
                .setOk(true)
                .setTotalQty(total)
                .setReservedQty(reserved)
                .setDeductedQty(deduced)
                .build());
        responseObserver.onCompleted();
    }
}
