-- Service combinations table (alternative packages)
CREATE TABLE IF NOT EXISTS quarkus.tour_service_combinations (
    id BIGSERIAL PRIMARY KEY,
    tour_id VARCHAR(36) NOT NULL REFERENCES quarkus.tours(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tour_service_combinations_tour ON quarkus.tour_service_combinations(tour_id);

-- Service combination items table (ordered services within a combination)
CREATE TABLE IF NOT EXISTS quarkus.tour_service_combination_items (
    id BIGSERIAL PRIMARY KEY,
    combination_id BIGINT NOT NULL REFERENCES quarkus.tour_service_combinations(id) ON DELETE CASCADE,
    service_name VARCHAR(255) NOT NULL,
    item_order INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tour_service_combination_items_combination ON quarkus.tour_service_combination_items(combination_id);
