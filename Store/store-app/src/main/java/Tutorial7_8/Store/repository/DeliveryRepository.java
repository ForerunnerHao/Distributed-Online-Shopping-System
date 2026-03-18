package Tutorial7_8.Store.repository;

import Tutorial7_8.Store.model.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.order.id = :orderId")
    Optional<Delivery> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    List<Delivery> findAllByUser_Id(Long userId);
}
