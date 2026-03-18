package Tutorial7_8.warehouse.config;

import Tutorial7_8.warehouse.model.WarehouseStock;
import Tutorial7_8.warehouse.repository.WarehouseStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class StockInitializer {

    private final WarehouseStockRepository stockRepository;

    @Value("${warehouse.id:WH-01}")
    private String warehouseId;

    @Bean
    public CommandLineRunner seedStock() {
        return args -> {
            List<WarehouseStock> predefined;

            // Different stock data for warehouse1 and warehouse2
            if ("WH-01".equals(warehouseId) || warehouseId.contains("1")) {
                // Warehouse 1 stock
                predefined = List.of(
                        createStock("UC-100", 100, 5, 15),
                        createStock("SWP-2025", 50, 2, 8),
                        createStock("DM-999", 10, 0, 0)
                );
            } else {
                // Warehouse 2 stock
                predefined = List.of(
                        createStock("UC-100", 120, 3, 10),
                        createStock("SWP-2025", 40, 1, 5),
                        createStock("DM-999", 5, 0, 0)
                );
            }

            for (WarehouseStock stock : predefined) {
                Optional<WarehouseStock> existing = stockRepository.findByItemSku(stock.getItemSku());
                if (existing.isPresent()) {
                    // Stock already exists, skip to preserve actual runtime data
                    log.debug("Stock {} already exists in warehouse {}, skipping initialization", 
                            stock.getItemSku(), warehouseId);
                } else {
                    stockRepository.save(stock);
                    log.info("Seeded stock {} in warehouse {}: total={}, reserved={}, deducted={}", 
                            stock.getItemSku(), warehouseId, 
                            stock.getTotalQty(), stock.getReservedQty(), stock.getDeductedQty());
                }
            }
        };
    }

    private WarehouseStock createStock(String itemSku, int totalQty, int reservedQty, int deductedQty) {
        return WarehouseStock.builder()
                .itemSku(itemSku)
                .totalQty(totalQty)
                .reservedQty(reservedQty)
                .deductedQty(deductedQty)
                .build();
    }
}

