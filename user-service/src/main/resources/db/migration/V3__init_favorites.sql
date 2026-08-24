-- user-service / user_db — favourites table.
--
-- Note: product_id is intentionally NOT a foreign key — products live in
-- product_db (a separate database); cross-service referential integrity is
-- enforced at the application level via OpenFeign lookups.

CREATE TABLE favorites (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_favorites_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_favorites_user_id    ON favorites(user_id);
CREATE INDEX idx_favorites_product_id ON favorites(product_id);