-- V1__initial_schema.sql
-- XorazmDelivery — asosiy ma'lumotlar bazasi sxemasi

-- Kengaytmalar
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================
-- ENUM turlari
-- ============================================================
CREATE TYPE user_role AS ENUM ('CUSTOMER', 'COURIER', 'FARMER', 'ADMIN');
CREATE TYPE order_status AS ENUM (
    'PENDING', 'SEARCHING', 'ASSIGNED', 'PICKED_UP', 'ON_THE_WAY', 'DELIVERED', 'CANCELLED', 'FAILED'
);
CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'REFUNDED');
CREATE TYPE payment_provider AS ENUM ('CLICK', 'PAYME', 'CASH');
CREATE TYPE subscription_plan AS ENUM ('FREE', 'PREMIUM', 'BUSINESS');
CREATE TYPE notification_channel AS ENUM ('SMS', 'TELEGRAM', 'PUSH');
CREATE TYPE notification_type AS ENUM (
    'ORDER_CREATED', 'ORDER_ASSIGNED', 'ORDER_PICKED_UP', 'ORDER_DELIVERED',
    'ORDER_CANCELLED', 'PAYMENT_SUCCESS', 'PAYMENT_FAILED', 'OTP', 'PROMO', 'SYSTEM'
);
CREATE TYPE vehicle_type AS ENUM ('BICYCLE', 'SCOOTER', 'MOTORCYCLE', 'CAR', 'TRUCK');

-- ============================================================
-- USERS — asosiy foydalanuvchilar jadvali
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone         VARCHAR(15) NOT NULL UNIQUE,
    full_name     VARCHAR(100),
    avatar_url    TEXT,
    role          user_role NOT NULL DEFAULT 'CUSTOMER',
    lang          VARCHAR(5) NOT NULL DEFAULT 'uz',
    telegram_id   BIGINT UNIQUE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    is_verified   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_telegram ON users(telegram_id);

-- ============================================================
-- COURIER_PROFILES — kuryer ma'lumotlari
-- ============================================================
CREATE TABLE courier_profiles (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id        UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type   vehicle_type NOT NULL DEFAULT 'BICYCLE',
    license_plate  VARCHAR(20),
    id_card_url    TEXT,
    license_url    TEXT,
    is_verified    BOOLEAN NOT NULL DEFAULT FALSE,
    is_online      BOOLEAN NOT NULL DEFAULT FALSE,
    rating         NUMERIC(3,2) NOT NULL DEFAULT 5.00,
    total_reviews  INTEGER NOT NULL DEFAULT 0,
    total_orders   INTEGER NOT NULL DEFAULT 0,
    balance        BIGINT NOT NULL DEFAULT 0,   -- so'mda, tiyin yo'q
    current_lat    DOUBLE PRECISION,
    current_lng    DOUBLE PRECISION,
    location_updated_at TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_courier_online ON courier_profiles(is_online) WHERE is_online = TRUE;
-- PostGIS spatial index (PostGIS o'rnatilgan bo'lsa qo'shing):
-- CREATE INDEX idx_courier_location ON courier_profiles USING GIST (
--     ST_SetSRID(ST_MakePoint(current_lng, current_lat), 4326)
-- ) WHERE is_online = TRUE AND current_lat IS NOT NULL;

-- ============================================================
-- ADDRESSES — saqlangan manzillar
-- ============================================================
CREATE TABLE addresses (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    label        VARCHAR(50) NOT NULL,       -- "Uy", "Ish", "Universitet"
    address_text TEXT NOT NULL,
    lat          DOUBLE PRECISION NOT NULL,
    lng          DOUBLE PRECISION NOT NULL,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_addresses_user ON addresses(user_id);

-- ============================================================
-- ORDERS — buyurtmalar
-- ============================================================
CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id         UUID NOT NULL REFERENCES users(id),
    courier_id          UUID REFERENCES users(id),

    status              order_status NOT NULL DEFAULT 'PENDING',

    pickup_address      TEXT NOT NULL,
    pickup_lat          DOUBLE PRECISION NOT NULL,
    pickup_lng          DOUBLE PRECISION NOT NULL,

    delivery_address    TEXT NOT NULL,
    delivery_lat        DOUBLE PRECISION NOT NULL,
    delivery_lng        DOUBLE PRECISION NOT NULL,

    distance_km         NUMERIC(8,2),
    weight_kg           NUMERIC(8,2) NOT NULL DEFAULT 1.0,
    is_fragile          BOOLEAN NOT NULL DEFAULT FALSE,
    description         TEXT,
    photo_url           TEXT,

    base_fee            BIGINT NOT NULL DEFAULT 0,
    total_fee           BIGINT NOT NULL,
    is_night_rate       BOOLEAN NOT NULL DEFAULT FALSE,
    is_holiday_rate     BOOLEAN NOT NULL DEFAULT FALSE,

    courier_search_started_at   TIMESTAMPTZ,
    assigned_at         TIMESTAMPTZ,
    picked_up_at        TIMESTAMPTZ,
    delivered_at        TIMESTAMPTZ,
    cancelled_at        TIMESTAMPTZ,
    cancel_reason       TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_courier ON orders(courier_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

-- ============================================================
-- PAYMENTS — to'lovlar
-- ============================================================
CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id        UUID NOT NULL REFERENCES orders(id),
    amount          BIGINT NOT NULL,
    provider        payment_provider NOT NULL,
    status          payment_status NOT NULL DEFAULT 'PENDING',
    transaction_id  VARCHAR(100),
    provider_data   JSONB,
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_payments_order ON payments(order_id);
CREATE INDEX idx_payments_transaction ON payments(transaction_id);

-- ============================================================
-- COURIER_LOCATIONS — kuryer harakati tarixi (partitioned)
-- ============================================================
CREATE TABLE courier_locations (
    id          UUID DEFAULT uuid_generate_v4(),
    courier_id  UUID NOT NULL REFERENCES users(id),
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    speed       NUMERIC(5,2),
    accuracy    NUMERIC(5,2),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (recorded_at);

CREATE TABLE courier_locations_2024 PARTITION OF courier_locations
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE courier_locations_2025 PARTITION OF courier_locations
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE courier_locations_2026 PARTITION OF courier_locations
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

CREATE INDEX idx_courier_locations_courier ON courier_locations(courier_id, recorded_at DESC);

-- ============================================================
-- REVIEWS — baholar
-- ============================================================
CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id    UUID NOT NULL UNIQUE REFERENCES orders(id),
    reviewer_id UUID NOT NULL REFERENCES users(id),
    target_id   UUID NOT NULL REFERENCES users(id),
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_target ON reviews(target_id);

-- ============================================================
-- SUBSCRIPTIONS — obunalar
-- ============================================================
CREATE TABLE subscriptions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan        subscription_plan NOT NULL DEFAULT 'FREE',
    started_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    payment_id  UUID REFERENCES payments(id)
);
CREATE INDEX idx_subscriptions_user ON subscriptions(user_id);
CREATE INDEX idx_subscriptions_active ON subscriptions(user_id, is_active) WHERE is_active = TRUE;

-- ============================================================
-- NOTIFICATIONS — xabarnomalar
-- ============================================================
CREATE TABLE notifications (
    id         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id    UUID NOT NULL REFERENCES users(id),
    type       notification_type NOT NULL,
    channel    notification_channel NOT NULL,
    title      VARCHAR(200),
    content    TEXT NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at    TIMESTAMPTZ,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);

-- ============================================================
-- PRICE_CONFIG — admin tomonidan sozlanuvchi narxlar
-- ============================================================
CREATE TABLE price_config (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    base_fee        BIGINT NOT NULL DEFAULT 5000,
    per_km_fee      BIGINT NOT NULL DEFAULT 500,
    per_kg_fee      BIGINT NOT NULL DEFAULT 200,
    min_fee         BIGINT NOT NULL DEFAULT 7000,
    night_multiplier    NUMERIC(4,2) NOT NULL DEFAULT 1.30,
    holiday_multiplier  NUMERIC(4,2) NOT NULL DEFAULT 1.50,
    bulk_weight_kg      NUMERIC(5,2) NOT NULL DEFAULT 10.0,
    bulk_discount       NUMERIC(4,2) NOT NULL DEFAULT 0.85,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by      UUID REFERENCES users(id)
);

-- Default narx konfiguratsiyasi
INSERT INTO price_config (base_fee, per_km_fee, per_kg_fee, min_fee)
VALUES (5000, 500, 200, 7000);

-- ============================================================
-- Trigger: updated_at avtomatik yangilash
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_orders_updated_at
    BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================
-- Trigger: review yozilganda courier ratingni yangilash
-- ============================================================
CREATE OR REPLACE FUNCTION update_courier_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE courier_profiles
    SET rating = (
        SELECT ROUND(AVG(rating)::NUMERIC, 2)
        FROM reviews
        WHERE target_id = NEW.target_id
    ),
    total_reviews = (
        SELECT COUNT(*) FROM reviews WHERE target_id = NEW.target_id
    )
    WHERE user_id = NEW.target_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_courier_rating
    AFTER INSERT ON reviews FOR EACH ROW EXECUTE FUNCTION update_courier_rating();
