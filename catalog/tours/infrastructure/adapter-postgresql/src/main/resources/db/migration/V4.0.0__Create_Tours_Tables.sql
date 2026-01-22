-- Tours table
CREATE TABLE IF NOT EXISTS quarkus.tours (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(37) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price_amount DECIMAL(10, 2) NOT NULL,
    base_price_currency VARCHAR(3) NOT NULL,
    duration_days INT NOT NULL,
    duration_nights INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tours_code ON quarkus.tours(code);
CREATE INDEX IF NOT EXISTS idx_tours_status ON quarkus.tours(status);

-- Tour included services table
CREATE TABLE IF NOT EXISTS quarkus.tour_included_services (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    base_price_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    base_price_currency VARCHAR(3) NOT NULL,
    tour_id VARCHAR(36) NOT NULL REFERENCES quarkus.tours(id) ON DELETE CASCADE,
    service_type VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tour_included_services_tour ON quarkus.tour_included_services(tour_id);

-- Tour policies table
CREATE TABLE IF NOT EXISTS quarkus.tour_policies (
    id BIGSERIAL PRIMARY KEY,
    tour_id VARCHAR(36) NOT NULL REFERENCES quarkus.tours(id) ON DELETE CASCADE,
    policy_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    policy_value VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_tour_policies_tour ON quarkus.tour_policies(tour_id);
CREATE INDEX IF NOT EXISTS idx_tour_policies_type ON quarkus.tour_policies(policy_type);
