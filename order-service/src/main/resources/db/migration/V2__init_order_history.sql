-- V2: order_history table
--
-- Audit trail of every status transition for each order. Cascades on delete
-- so removing an order also drops its history.

CREATE TABLE order_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status      VARCHAR(20)  NOT NULL,
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    changed_by  BIGINT,
    reason      VARCHAR(500)
);

CREATE INDEX idx_order_history_order_id ON order_history (order_id);
