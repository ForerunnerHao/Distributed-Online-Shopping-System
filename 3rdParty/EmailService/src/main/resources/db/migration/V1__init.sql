-- Create email_template table
CREATE TABLE IF NOT EXISTS email_template (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    subject_tpl TEXT,
    body_tpl TEXT,
    version INTEGER,
    enabled BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create email_log table
CREATE TABLE IF NOT EXISTS email_log (
    id UUID PRIMARY KEY,
    order_id VARCHAR(64),
    to_email VARCHAR(255),
    event_type VARCHAR(64),
    subject TEXT,
    body TEXT,
    status VARCHAR(16),
    provider_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);

-- Create recipient table
CREATE TABLE IF NOT EXISTS recipient (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(128),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert email templates
INSERT INTO email_template (id, code, subject_tpl, body_tpl, version, enabled, updated_at)
VALUES
    (gen_random_uuid(), 'DELIVERY_PICKED_UP', 
     'Your Order {{orderId}} Has Been Picked Up', 
     'Hello {{customerName}}, your order {{orderId}} has been picked up from the warehouse {{warehouse}}. Your tracking number is {{trackingNo}}.', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'DELIVERY_ON_TRUCK', 
     'Your Order {{orderId}} Is Now In Transit', 
     'Hello {{customerName}}, your order {{orderId}} is now in transit to you. Tracking number: {{trackingNo}}. Expected delivery within 2-3 business days.', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'DELIVERED', 
     'Your Order {{orderId}} Has Been Delivered', 
     'Hello {{customerName}}, your order {{orderId}} has been successfully delivered. We hope you enjoy your purchase!', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'ORDER_CANCELLED', 
     'Your Order {{orderId}} Has Been Cancelled', 
     'Hello {{customerName}}, your order {{orderId}} has been cancelled. If you have any questions, please contact our support team.', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'ORDER_FAILED', 
     'Your Order {{orderId}} Could Not Be Processed', 
     'Hello {{customerName}}, we encountered an issue processing your order {{orderId}}. Our team has been notified and will investigate. You will receive a follow-up email shortly.', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'REFUND_INITIATED', 
     'Refund Processing Started for Order {{orderId}}', 
     'Hello {{customerName}}, we have initiated the refund process for your order {{orderId}}. The refund will be processed to your original payment method within 5-10 business days.', 
     1, true, CURRENT_TIMESTAMP),
    
    (gen_random_uuid(), 'REFUND_COMPLETED', 
     'Refund Completed for Order {{orderId}}', 
     'Hello {{customerName}}, the refund for your order {{orderId}} has been successfully processed. The funds should appear in your account within 3-5 business days.', 
     1, true, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_email_log_order_id ON email_log(order_id);
CREATE INDEX IF NOT EXISTS idx_email_log_event_type ON email_log(event_type);
CREATE INDEX IF NOT EXISTS idx_email_log_status ON email_log(status);
CREATE INDEX IF NOT EXISTS idx_email_log_to_email ON email_log(to_email);
CREATE INDEX IF NOT EXISTS idx_email_log_created_at ON email_log(created_at);

