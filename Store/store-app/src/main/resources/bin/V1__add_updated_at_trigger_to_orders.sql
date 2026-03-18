-- V1__add_updated_at_trigger_to_orders.sql

-- create updated_at trigger function
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- for other tables to create trigger
-- 1. orders
DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;
CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION set_updated_at();

-- items
DROP TRIGGER IF EXISTS trg_items_updated_at ON items;
CREATE TRIGGER trg_items_updated_at
    BEFORE UPDATE ON items
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


-- outbox_events
DROP TRIGGER IF EXISTS trg_outbox_events_updated_at ON outbox_events;
CREATE TRIGGER trg_outbox_events_updated_at
    BEFORE UPDATE ON outbox_events
    FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
