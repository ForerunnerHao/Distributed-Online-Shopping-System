-- V2__create_orders_expires_trigger.sql

-- 1. Make sure created_at has a default value
-- (idempotent: if a default value already exists, no error will be reported)
ALTER TABLE orders
    ALTER COLUMN created_at SET DEFAULT now();

-- 2. Create a trigger function (use CREATE OR REPLACE to ensure idempotence)
CREATE OR REPLACE FUNCTION set_order_expires_at()
RETURNS TRIGGER AS $$
BEGIN
    -- If expires_at is not provided, based on created_at or
    -- current time + 15 minutes
    IF NEW.expires_at IS NULL THEN
    NEW.expires_at := COALESCE(NEW.created_at, now()) + interval '15 minutes';
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 3. Delete the old trigger if it exists
DROP TRIGGER IF EXISTS trg_orders_set_expires ON orders;

-- 4. Create new trigger
CREATE TRIGGER trg_orders_set_expires
    BEFORE INSERT ON orders
    FOR EACH ROW
    EXECUTE FUNCTION set_order_expires_at();