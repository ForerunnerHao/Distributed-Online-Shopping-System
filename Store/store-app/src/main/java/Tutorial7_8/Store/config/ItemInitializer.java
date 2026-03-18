package Tutorial7_8.Store.config;

import Tutorial7_8.Store.model.Item;
import Tutorial7_8.Store.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemInitializer {

    private final ItemRepository itemRepository;

    @Bean
    public CommandLineRunner seedItems() {
        return args -> {
            List<Item> predefined = List.of(
                    createItem("UC-100", "USB-C Charging Cable", new BigDecimal("12.50"), true),
                    createItem("SWP-2025", "Smart Watch Pro", new BigDecimal("199.99"), true),
                    createItem("DM-999", "Discontinued Mouse", new BigDecimal("25.00"), false)
            );

            for (Item item : predefined) {
                Item existing = itemRepository.findItemBySku(item.getSku());
                if (existing != null) {
                    // Update existing item if needed
                    if (!existing.isActive() && item.isActive()) {
                        existing.setActive(true);
                        itemRepository.save(existing);
                        log.info("Updated item {}", item.getSku());
                    }
                } else {
                    itemRepository.save(item);
                    log.info("Seeded item {}", item.getSku());
                }
            }
        };
    }

    private Item createItem(String sku, String name, BigDecimal price, boolean active) {
        return Item.builder()
                .sku(sku)
                .name(name)
                .price(price)
                .active(active)
                .build();
    }
}

