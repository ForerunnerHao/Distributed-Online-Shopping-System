package Tutorial7_8.Store.repository;

import Tutorial7_8.Store.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Item findItemBySku(String sku);
}
