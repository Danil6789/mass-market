-- product-service / product_db — initial products table.
--
-- Notes:
--  * seller_id references user-service but is intentionally NOT a foreign key
--    — users live in user_db (a separate database); cross-service referential
--    integrity is enforced at the application level via OpenFeign lookups.
--  * category_id references categories table in this same database.

CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    seller_id    BIGINT        NOT NULL,
    category_id  BIGINT        NOT NULL,
    title        VARCHAR(200)  NOT NULL,
    description  VARCHAR(2000),
    price        NUMERIC(12, 2) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ,
    CONSTRAINT chk_products_price  CHECK (price >= 0),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'RESERVED', 'SOLD', 'DELETED'))
);

CREATE INDEX idx_products_seller_id   ON products(seller_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status      ON products(status);
CREATE INDEX idx_products_title       ON products(title);