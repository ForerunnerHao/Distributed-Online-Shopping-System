package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.payment.RefundRequest;
import Tutorial7_8.Store.dto.payment.bank.BankCallbackRequest;
import Tutorial7_8.Store.dto.payment.PaymentDTO;
import Tutorial7_8.Store.dto.payment.PaymentRequest;
import Tutorial7_8.Store.error.exception.BusinessException;
import Tutorial7_8.Store.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Relative payment API endpoints")
public class PaymentController {

    final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("{payment_id}")
    public ResponseEntity<PaymentDTO> getPayment(@PathVariable String payment_id, HttpServletRequest req) {
        String userId =  (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        PaymentDTO payment = paymentService.getPaymentByPaymentId(Long.parseLong(payment_id));

        return ResponseEntity.ok(payment);
    }


    @GetMapping()
    public ResponseEntity<List<PaymentDTO>> getPayments(HttpServletRequest req) {
        String userId =  (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }
        List<PaymentDTO> payments = paymentService.getPaymentsByUserId(Long.valueOf(userId));

        return ResponseEntity.ok(payments);
    }


    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest req) {
        log.info("createPayment /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }

        PaymentDTO payment = paymentService.payOrder(paymentRequest, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("refund")
    public ResponseEntity<PaymentDTO> refundPayment(@RequestBody RefundRequest refundRequest, HttpServletRequest req) {
        log.info("refundPayment /demo");
        String userId = (String) req.getAttribute("auth.userId");
        if (userId == null) {
            throw new BusinessException("AUTHORIZED_ERROR", "Please login first", HttpStatus.UNAUTHORIZED);
        }

        PaymentDTO payment = paymentService.refund(refundRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }


    @PostMapping("callback")
    public ResponseEntity<BankCallbackRequest> callbackPayment(@RequestBody BankCallbackRequest callbackRequest) {
        log.info("callbackPayment /demo");
        paymentService.bankCallback(callbackRequest);

        // return the callback to bank app
        return ResponseEntity.ok().body(callbackRequest);
    }
}
