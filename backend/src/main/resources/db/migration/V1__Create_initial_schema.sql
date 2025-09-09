-- Create customers table
CREATE TABLE customers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email_masked VARCHAR(255) NOT NULL,
    risk_flags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create cards table
CREATE TABLE cards (
    id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL REFERENCES customers(id),
    last4 VARCHAR(4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    network VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create devices table
CREATE TABLE devices (
    id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL REFERENCES customers(id),
    device_type VARCHAR(50) NOT NULL,
    os VARCHAR(100),
    browser VARCHAR(100),
    fingerprint VARCHAR(255),
    last_seen TIMESTAMP WITH TIME ZONE,
    is_trusted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create transactions table with partitioning
CREATE TABLE transactions (
    id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(50) NOT NULL REFERENCES customers(id),
    card_id VARCHAR(50) NOT NULL REFERENCES cards(id),
    mcc VARCHAR(10) NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL, -- Amount in smallest currency unit (paise for INR)
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    device_id VARCHAR(50) REFERENCES devices(id),
    geo_lat DECIMAL(10, 8),
    geo_lon DECIMAL(11, 8),
    geo_country VARCHAR(3),
    geo_city VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'captured',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, ts)
) PARTITION BY RANGE (ts);

-- Create monthly partitions for transactions (example for 2024-2025)
CREATE TABLE transactions_2024_01 PARTITION OF transactions
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE transactions_2024_02 PARTITION OF transactions
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
CREATE TABLE transactions_2024_03 PARTITION OF transactions
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
CREATE TABLE transactions_2024_04 PARTITION OF transactions
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
CREATE TABLE transactions_2024_05 PARTITION OF transactions
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');
CREATE TABLE transactions_2024_06 PARTITION OF transactions
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');
CREATE TABLE transactions_2024_07 PARTITION OF transactions
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');
CREATE TABLE transactions_2024_08 PARTITION OF transactions
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');
CREATE TABLE transactions_2024_09 PARTITION OF transactions
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');
CREATE TABLE transactions_2024_10 PARTITION OF transactions
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');
CREATE TABLE transactions_2024_11 PARTITION OF transactions
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
CREATE TABLE transactions_2024_12 PARTITION OF transactions
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');
CREATE TABLE transactions_2025_01 PARTITION OF transactions
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
CREATE TABLE transactions_2025_02 PARTITION OF transactions
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');
CREATE TABLE transactions_2025_03 PARTITION OF transactions
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

-- Create chargebacks table
CREATE TABLE chargebacks (
    id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL REFERENCES customers(id),
    transaction_id VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    reason_code VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE
);

-- Create knowledge base documents table
CREATE TABLE kb_documents (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    anchor VARCHAR(100) NOT NULL,
    content JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create agent traces table
CREATE TABLE agent_traces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    customer_id VARCHAR(50),
    trace_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create action logs table
CREATE TABLE action_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id VARCHAR(100) NOT NULL,
    session_id VARCHAR(100),
    customer_id VARCHAR(50),
    action_type VARCHAR(50) NOT NULL,
    action_data JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_transactions_customer_ts ON transactions (customer_id, ts DESC);
CREATE INDEX idx_transactions_merchant ON transactions (merchant);
CREATE INDEX idx_transactions_mcc ON transactions (mcc);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_amount ON transactions (amount);

CREATE INDEX idx_cards_customer ON cards (customer_id);
CREATE INDEX idx_devices_customer ON devices (customer_id);
CREATE INDEX idx_chargebacks_customer ON chargebacks (customer_id);
CREATE INDEX idx_chargebacks_status ON chargebacks (status);

CREATE INDEX idx_agent_traces_request ON agent_traces (request_id);
CREATE INDEX idx_agent_traces_customer ON agent_traces (customer_id);
CREATE INDEX idx_agent_traces_created ON agent_traces (created_at);

CREATE INDEX idx_action_logs_request ON action_logs (request_id);
CREATE INDEX idx_action_logs_customer ON action_logs (customer_id);
CREATE INDEX idx_action_logs_action_type ON action_logs (action_type);
CREATE INDEX idx_action_logs_created ON action_logs (created_at);

-- Create updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
CREATE TRIGGER update_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cards_updated_at BEFORE UPDATE ON cards
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_kb_documents_updated_at BEFORE UPDATE ON kb_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
