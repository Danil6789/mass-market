-- product-service / product_db — initial product_images table.
--
-- Note: product_id references products in this same database; we keep the
-- FK lightweight (no DB-level FK constraint) so the soft-delete lifecycle
-- of products can be managed freely.

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT       NOT NULL,
    url         VARCHAR(500) NOT NULL,
    position    INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_images_position CHECK (position >= 0)
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);