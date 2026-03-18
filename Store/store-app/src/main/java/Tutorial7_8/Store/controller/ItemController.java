package Tutorial7_8.Store.controller;

import Tutorial7_8.Store.dto.item.ItemDTO;
import Tutorial7_8.Store.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/items")
@Tag(name = "Item", description = "Relative item API endpoints")
public class ItemController {
    private final InventoryService inventoryService;

    public ItemController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("{item_id}")
    public ResponseEntity<ItemDTO> getItem(@PathVariable String item_id) {
        ItemDTO item = inventoryService.getItemStockById(Long.parseLong(item_id));
        return ResponseEntity.ok(item);
    }

    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        List<ItemDTO> items = inventoryService.getAllItems();
        return ResponseEntity.ok(items);
    }
}
