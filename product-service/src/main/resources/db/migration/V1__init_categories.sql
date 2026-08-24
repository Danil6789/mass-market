-- product-service / product_db — initial categories table.
-- Created in Phase 4 of the MassMarket plan.

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    parent_id   BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_categories_parent_name UNIQUE (parent_id, name)
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);