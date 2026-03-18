package Tutorial7_8.warehouse.repository;

import Tutorial7_8.warehouse.model.WarehouseStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    Optional<WarehouseStock> findByItemSku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ws FROM WarehouseStock ws WHERE ws.itemSku = :itemSku")
    Optional<WarehouseStock> findByItemSkuWithLock(@Param("itemSku") String itemSku);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WarehouseStock ws SET ws.reservedQty = ws.reservedQty + :reserveQty" +
            ", ws.deductedQty = ws.deductedQty + :deductedQty, ws.version = ws.version + 1 " +
            "WHERE ws.id = :id AND ws.version = :version")
    int updateStockWithVersion(@Param("id") Long id,
                               @Param("reserveQty") int reserveQty,
                               @Param("deductedQty") int deductedQty,
                               @Param("version") Long version);

}
