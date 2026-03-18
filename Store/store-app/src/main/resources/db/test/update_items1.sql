UPDATE public.items
SET name    = 'Wireless Bluetooth Headphones Pro',
    price   = 69.99,
    active  = true,
    version = version + 1
WHERE sku = 'UC-100'
  AND version = 0; -- Optimistic locking: ensures that the version has not been modified by other transactions