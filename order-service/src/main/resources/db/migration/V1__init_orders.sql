-- V1: orders table
--
-- One row per order in the marketplace. buyer_id and seller_id are bare
-- bigint — no cross-table FK constraints (each service owns its own DB).
-- Lifecycle: PENDING → PAID (happy path), PENDING → CANCELLED, or
-- PENDING → FAILED (payment service could not process).

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    buyer_id        BIGINT       NOT NULL,
    seller_id       BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING','PAID','CANCELLED','FAILED')),
    payment_id      VARCHAR(100),
    paid_at         TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    cancelled_by    BIGINT,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_buyer_id  ON orders (buyer_id);
CREATE INDEX idx_orders_seller_id ON orders (seller_id);
CREATE INDEX idx_orders_product_id ON orders (product_id);
CREATE INDEX idx_orders_status    ON orders (status);
