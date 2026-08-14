-- Online Bookshop schema
-- Schema is owned by Flyway; Hibernate runs in validate-only mode.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(120) NOT NULL,
    -- BCrypt hash; NULL for social-login accounts that have no local password.
    password_hash VARCHAR(100),
    -- Which identity provider owns this account: LOCAL | GOOGLE | FACEBOOK
    auth_provider VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    -- The stable user id issued by the social provider ("sub" for Google, "id" for Facebook).
    provider_id   VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_auth_provider CHECK (auth_provider IN ('LOCAL', 'GOOGLE', 'FACEBOOK')),
    -- A LOCAL account must have a password; a social account must have a provider id.
    CONSTRAINT chk_credentials CHECK (
        (auth_provider = 'LOCAL' AND password_hash IS NOT NULL)
        OR (auth_provider <> 'LOCAL' AND provider_id IS NOT NULL)
    ),
    CONSTRAINT uq_provider_identity UNIQUE (auth_provider, provider_id)
);

CREATE TABLE books (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255)   NOT NULL,
    author      VARCHAR(255)   NOT NULL,
    pages       INTEGER        NOT NULL CHECK (pages > 0),
    -- Nullable by design: the UI falls back to a generated cover when absent.
    image_url   VARCHAR(1024),
    price       NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock       INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_books_title ON books (lower(title));

-- Active cart: one row per (user, book). Checkout converts these into an order.
CREATE TABLE cart_items (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    added_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_user_book UNIQUE (user_id, book_id)
);

CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status              VARCHAR(20)    NOT NULL DEFAULT 'CONFIRMED',
    total_amount        NUMERIC(10, 2) NOT NULL CHECK (total_amount >= 0),
    shipping_name       VARCHAR(120)   NOT NULL,
    shipping_address    VARCHAR(500)   NOT NULL,
    -- Mocked payment: we store a fake confirmation reference, never card data.
    payment_reference   VARCHAR(64)    NOT NULL,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_order_status CHECK (status IN ('CONFIRMED', 'SHIPPED', 'CANCELLED'))
);

-- Immutable snapshot of what was bought and at which price at checkout time,
-- so later catalog price changes never rewrite order history.
CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    book_id      BIGINT         NOT NULL REFERENCES books (id),
    title        VARCHAR(255)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    quantity     INTEGER        NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_cart_items_user ON cart_items (user_id);
CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_order_items_order ON order_items (order_id);
