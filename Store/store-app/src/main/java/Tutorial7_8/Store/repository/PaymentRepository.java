package Tutorial7_8.Store.repository;

import Tutorial7_8.Store.model.Order;
import Tutorial7_8.Store.model.Payment;
import Tutorial7_8.Store.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByOrder(Order order);

    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    Payment findPaymentByIdempotencyKey(String idempotencyKey);

    List<Payment> findByUser(User user);
}
