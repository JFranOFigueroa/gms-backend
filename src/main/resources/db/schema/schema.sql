--liquibase formatted sql

-- GMS | Esquema relacional consolidado v1.0 (baseline Liquibase)
-- Origen: migraciones Flyway V1-V6 conservadas en db/migration/
-- Motor objetivo: PostgreSQL 15+ | Schema configurable: ${schema_db}
-- ADVERTENCIA: Esquema inicial. La API Java debe comprobar permisos por gimnasio;
-- no conectar el frontend directamente a esta base de datos.
-- Esta migracion NO crea cuentas ni contrasenas de demostracion.
--
-- Este archivo es un baseline inmutable una vez aplicado. Los cambios posteriores
-- deben agregarse como nuevos changesets/changelogs, no editando este archivo.
--
-- splitStatements:false es intencional: los bloques DO/PLpgSQL contienen ';' internos.
-- Si Liquibase no pudiera ejecutar el archivo completo como una sola sentencia,
-- dividir este changeset internamente usando endDelimiter:@@.

--changeset gms:schema-001 splitStatements:false dbms:postgresql

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;
CREATE SCHEMA IF NOT EXISTS ${schema_db};
SET LOCAL search_path = ${schema_db}, public;

-- ================================ 01. IDENTIDAD Y MULTI-GIMNASIO
CREATE TABLE ${schema_db}.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name TEXT NOT NULL CHECK (length(trim(full_name)) > 0),
    email CITEXT NOT NULL UNIQUE,
    phone TEXT,
    password_hash TEXT, -- Argon2id/bcrypt, NUNCA texto plano
    status TEXT NOT NULL DEFAULT 'invited' CHECK (status IN ('invited','active','disabled')),
    default_gym_id UUID, -- FK se agrega despues de declarar gym
    email_verified_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    CONSTRAINT ck_user_password CHECK (status = 'invited' OR password_hash IS NOT NULL)
);

CREATE TABLE ${schema_db}.gym (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    short_name TEXT,
    slug CITEXT NOT NULL UNIQUE,
    address TEXT,
    phone TEXT,
    logo_url TEXT,
    timezone TEXT NOT NULL DEFAULT 'America/Mexico_City',
    currency CHAR(3) NOT NULL DEFAULT 'MXN',
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','suspended','archived')),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);

ALTER TABLE ${schema_db}.users
    ADD CONSTRAINT fk_users_default_gym FOREIGN KEY (default_gym_id)
    REFERENCES ${schema_db}.gym(id) ON DELETE SET NULL;

-- Rol GLOBAL de soporte/administracion tecnica. No se asigna por gimnasio.
CREATE TABLE ${schema_db}.platform_admins (
    user_id UUID PRIMARY KEY REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    assigned_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);

-- Un mismo usuario puede ser owner de varios gimnasios o trainer en otros.
CREATE TABLE ${schema_db}.gym_staff_roles (
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    role TEXT NOT NULL CHECK (role IN ('owner','trainer')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    PRIMARY KEY (gym_id, user_id, role)
);
CREATE INDEX ix_staff_user ON ${schema_db}.gym_staff_roles(user_id, gym_id) WHERE is_active;

CREATE TABLE ${schema_db}.user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES ${schema_db}.users(id) ON DELETE CASCADE,
    refresh_token_hash TEXT NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    CONSTRAINT ck_session_times CHECK (expires_at > issued_at)
);
CREATE INDEX ix_sessions_user_active ON ${schema_db}.user_sessions(user_id, expires_at)
    WHERE revoked_at IS NULL;

-- ================================ 02. CONFIGURACION POR GIMNASIO
CREATE TABLE ${schema_db}.gym_settings (
    gym_id UUID PRIMARY KEY REFERENCES ${schema_db}.gym(id) ON DELETE CASCADE,
    theme_preset TEXT NOT NULL DEFAULT 'crossfit-dark',
    theme_overrides JSONB NOT NULL DEFAULT '{}'::jsonb,
    allow_multiple_normal_classes_per_day BOOLEAN NOT NULL DEFAULT FALSE,
    guest_booking_limit INTEGER NOT NULL DEFAULT 1 CHECK (guest_booking_limit >= 0),
    checkin_qr_ttl_seconds INTEGER NOT NULL DEFAULT 10 CHECK (checkin_qr_ttl_seconds BETWEEN 5 AND 300),
    landing_published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);

-- Solo modulos configurables. users/members/gym son modulos estructurales.
CREATE TABLE ${schema_db}.gym_modules (
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE CASCADE,
    module_key TEXT NOT NULL CHECK (module_key IN ('calendar','routines','checkin','pos')),
    enabled BOOLEAN NOT NULL,
    options JSONB NOT NULL DEFAULT '{}'::jsonb,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    PRIMARY KEY (gym_id, module_key)
);

CREATE TABLE ${schema_db}.media_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    media_type TEXT NOT NULL CHECK (media_type IN ('image','video','document')),
    storage_key TEXT, -- clave de objeto privado en almacenamiento externo
    external_url TEXT,
    mime_type TEXT,
    byte_size BIGINT CHECK (byte_size IS NULL OR byte_size >= 0),
    uploaded_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    CONSTRAINT ck_asset_source CHECK (num_nonnulls(storage_key, external_url) = 1)
);
CREATE INDEX ix_media_gym ON ${schema_db}.media_assets(gym_id, created_at DESC);

CREATE TABLE ${schema_db}.landing_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    block_type TEXT NOT NULL, -- hero, text, carousel, membership, etc.
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    block_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id)
);
CREATE INDEX ix_landing_order ON ${schema_db}.landing_blocks(gym_id, sort_order);

-- ================================ 03. MIEMBROS Y MEMBRESIAS
-- La cuenta (users) es global; el expediente se repite por gimnasio.
CREATE TABLE ${schema_db}.gym_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    member_code TEXT NOT NULL CHECK (length(trim(member_code)) > 0),
    birth_date DATE,
    address TEXT,
    emergency_contact_name TEXT,
    emergency_contact_phone TEXT,
    photo_asset_id UUID,
    notes TEXT,
    joined_on DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, user_id),
    UNIQUE (gym_id, member_code),
    FOREIGN KEY (gym_id, photo_asset_id) REFERENCES ${schema_db}.media_assets(gym_id, id)
);
CREATE INDEX ix_members_gym_active ON ${schema_db}.gym_members(gym_id, is_active);

CREATE TABLE ${schema_db}.membership_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    billing_period TEXT NOT NULL CHECK (billing_period IN ('monthly','yearly')),
    interval_count INTEGER NOT NULL DEFAULT 1 CHECK (interval_count >= 1),
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, name)
);

CREATE TABLE ${schema_db}.member_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    member_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    started_on DATE NOT NULL,
    next_due_date DATE NOT NULL,
    last_payment_date DATE,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active','paused','cancelled')),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, id, member_id),
    FOREIGN KEY (gym_id, member_id) REFERENCES ${schema_db}.gym_members(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, plan_id) REFERENCES ${schema_db}.membership_plans(gym_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_subscription_due CHECK (next_due_date >= started_on)
);
-- Una sola suscripcion activa por miembro y gimnasio.
CREATE UNIQUE INDEX ux_one_active_subscription
    ON ${schema_db}.member_subscriptions(gym_id, member_id) WHERE status = 'active';

CREATE TABLE ${schema_db}.membership_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    member_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    method TEXT NOT NULL CHECK (method IN ('cash','card','transfer','other')),
    concept TEXT NOT NULL DEFAULT 'membership',
    periodicity TEXT NOT NULL CHECK (periodicity IN ('monthly','yearly')),
    periods INTEGER NOT NULL DEFAULT 1 CHECK (periods >= 1),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    period_start DATE NOT NULL,
    next_due_date DATE NOT NULL,
    status TEXT NOT NULL DEFAULT 'paid' CHECK (status IN ('paid','voided','refunded')),
    external_reference TEXT,
    notes TEXT,
    registered_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    FOREIGN KEY (gym_id, subscription_id, member_id)
        REFERENCES ${schema_db}.member_subscriptions(gym_id, id, member_id) ON DELETE RESTRICT,
    CONSTRAINT ck_payment_period CHECK (next_due_date > period_start)
);
CREATE INDEX ix_payments_member ON ${schema_db}.membership_payments(gym_id, member_id, paid_at DESC);
CREATE INDEX ix_payments_gym ON ${schema_db}.membership_payments(gym_id, paid_at DESC);

-- ================================ 04. CALENDARIO Y RESERVAS
CREATE TABLE ${schema_db}.class_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'normal'
        CHECK (category IN ('normal','special','overlay_special','event')),
    color TEXT,
    duration_minutes INTEGER NOT NULL DEFAULT 60 CHECK (duration_minutes > 0),
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id)
);

CREATE TABLE ${schema_db}.gym_classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    template_id UUID,
    name TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('normal','special','overlay_special','event')),
    color TEXT,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    trainer_user_id UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'scheduled' CHECK (status IN ('scheduled','cancelled','completed')),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    FOREIGN KEY (gym_id, template_id) REFERENCES ${schema_db}.class_templates(gym_id, id),
    CONSTRAINT ck_class_time CHECK (ends_at > starts_at)
);
CREATE INDEX ix_classes_calendar ON ${schema_db}.gym_classes(gym_id, starts_at)
    WHERE status = 'scheduled';

-- Invitado NO es un quinto rol autenticado: es un pase temporal por gimnasio.
CREATE TABLE ${schema_db}.guest_passes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    full_name TEXT NOT NULL,
    phone_normalized TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'issued' CHECK (status IN ('issued','used','expired','revoked')),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, phone_normalized)
);

-- Un usuario autenticado puede reservar (member o staff); o un invitado con pase.
CREATE TABLE ${schema_db}.class_reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    class_id UUID NOT NULL,
    user_id UUID REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    guest_pass_id UUID,
    status TEXT NOT NULL DEFAULT 'booked'
        CHECK (status IN ('booked','cancelled','attended','no_show')),
    booked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    FOREIGN KEY (gym_id, class_id) REFERENCES ${schema_db}.gym_classes(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, guest_pass_id) REFERENCES ${schema_db}.guest_passes(gym_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_reservation_identity CHECK (num_nonnulls(user_id, guest_pass_id) = 1)
);
CREATE UNIQUE INDEX ux_active_reservation_user
    ON ${schema_db}.class_reservations(gym_id, class_id, user_id)
    WHERE user_id IS NOT NULL AND status IN ('booked','attended');
CREATE UNIQUE INDEX ux_active_reservation_guest
    ON ${schema_db}.class_reservations(gym_id, class_id, guest_pass_id)
    WHERE guest_pass_id IS NOT NULL AND status IN ('booked','attended');
CREATE INDEX ix_reservations_class ON ${schema_db}.class_reservations(gym_id, class_id, status);
CREATE INDEX ix_reservations_user ON ${schema_db}.class_reservations(user_id, booked_at DESC);

-- ================================ 05. RUTINAS GENERALES
CREATE TABLE ${schema_db}.routines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    name TEXT NOT NULL,
    description TEXT,
    level TEXT NOT NULL DEFAULT 'beginner' CHECK (level IN ('beginner','intermediate','advanced')),
    goal TEXT,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id)
);
CREATE INDEX ix_routines_published ON ${schema_db}.routines(gym_id, published);

CREATE TABLE ${schema_db}.routine_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    routine_id UUID NOT NULL,
    day_order INTEGER NOT NULL CHECK (day_order >= 1),
    day_label TEXT NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, routine_id, day_order),
    FOREIGN KEY (gym_id, routine_id) REFERENCES ${schema_db}.routines(gym_id, id) ON DELETE CASCADE
);

CREATE TABLE ${schema_db}.routine_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    routine_day_id UUID NOT NULL,
    exercise_order INTEGER NOT NULL CHECK (exercise_order >= 1),
    exercise_name TEXT NOT NULL,
    sets_label TEXT, -- el frontend permite texto libre
    reps_label TEXT, -- p.ej. '12', '45s', 'al fallo'
    rest_label TEXT, -- p.ej. '60s'
    notes TEXT,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, routine_day_id, exercise_order),
    FOREIGN KEY (gym_id, routine_day_id) REFERENCES ${schema_db}.routine_days(gym_id, id) ON DELETE CASCADE
);

-- ================================ 06. PUNTO DE VENTA E INVENTARIO
CREATE TABLE ${schema_db}.products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    sku TEXT NOT NULL,
    barcode TEXT,
    name TEXT NOT NULL,
    category TEXT,
    price NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, sku)
);
CREATE UNIQUE INDEX ux_product_barcode ON ${schema_db}.products(gym_id, barcode)
    WHERE barcode IS NOT NULL;

CREATE TABLE ${schema_db}.pos_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    total NUMERIC(12,2) NOT NULL CHECK (total >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'MXN',
    status TEXT NOT NULL DEFAULT 'completed' CHECK (status IN ('completed','voided','refunded')),
    sold_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    sold_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id)
);
CREATE INDEX ix_sales_date ON ${schema_db}.pos_sales(gym_id, sold_at DESC);

CREATE TABLE ${schema_db}.pos_sale_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    sale_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name TEXT NOT NULL, -- snapshot, no cambia al editar catalogo
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    line_total NUMERIC(12,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    FOREIGN KEY (gym_id, sale_id) REFERENCES ${schema_db}.pos_sales(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, product_id) REFERENCES ${schema_db}.products(gym_id, id) ON DELETE RESTRICT
);
CREATE INDEX ix_sale_items_sale ON ${schema_db}.pos_sale_items(gym_id, sale_id);

CREATE TABLE ${schema_db}.inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    product_id UUID NOT NULL,
    stock_delta INTEGER NOT NULL CHECK (stock_delta <> 0),
    reason TEXT NOT NULL CHECK (reason IN ('sale','receipt','adjustment','return','refund')),
    sale_id UUID,
    notes TEXT,
    performed_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    FOREIGN KEY (gym_id, product_id) REFERENCES ${schema_db}.products(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, sale_id) REFERENCES ${schema_db}.pos_sales(gym_id, id) ON DELETE RESTRICT
);
CREATE INDEX ix_movements_product ON ${schema_db}.inventory_movements(gym_id, product_id, created_at DESC);

-- ================================ 07. KIOSCOS, QR Y AUDITORIA DE ENTRADAS
CREATE TABLE ${schema_db}.checkin_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    device_name TEXT NOT NULL,
    device_secret_hash TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    UNIQUE (gym_id, id),
    UNIQUE (gym_id, device_name)
);

-- El token de 10 segundos se verifica en backend; solo se conserva su hash.
CREATE TABLE ${schema_db}.checkin_qr_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    device_id UUID NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    FOREIGN KEY (gym_id, device_id) REFERENCES ${schema_db}.checkin_devices(gym_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_qr_expiry CHECK (expires_at > issued_at)
);
CREATE INDEX ix_qr_cleanup ON ${schema_db}.checkin_qr_tokens(expires_at);

CREATE TABLE ${schema_db}.checkin_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    member_id UUID,
    guest_pass_id UUID,
    staff_user_id UUID REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    device_id UUID,
    method TEXT NOT NULL CHECK (method IN ('kiosk_code','kiosk_qr','member_qr','manual','guest')),
    allowed BOOLEAN NOT NULL,
    reason_code TEXT NOT NULL, -- granted, expired, inactive, duplicate, wrong_gym, etc.
    reason_detail TEXT,
    visit_date DATE NOT NULL, -- fecha LOCAL del gimnasio, no la del servidor
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    FOREIGN KEY (gym_id, member_id) REFERENCES ${schema_db}.gym_members(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, guest_pass_id) REFERENCES ${schema_db}.guest_passes(gym_id, id) ON DELETE RESTRICT,
    FOREIGN KEY (gym_id, device_id) REFERENCES ${schema_db}.checkin_devices(gym_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_checkin_identity CHECK (
        num_nonnulls(member_id, guest_pass_id, staff_user_id) <= 1
        AND (NOT allowed OR num_nonnulls(member_id, guest_pass_id, staff_user_id) = 1)
    )
);
-- Impide dos accesos concedidos a un miembro el mismo dia/gimnasio.
CREATE UNIQUE INDEX ux_member_daily_entry ON ${schema_db}.checkin_events(gym_id, member_id, visit_date)
    WHERE allowed AND member_id IS NOT NULL;
CREATE UNIQUE INDEX ux_guest_daily_entry ON ${schema_db}.checkin_events(gym_id, guest_pass_id, visit_date)
    WHERE allowed AND guest_pass_id IS NOT NULL;
CREATE UNIQUE INDEX ux_staff_daily_entry ON ${schema_db}.checkin_events(gym_id, staff_user_id, visit_date)
    WHERE allowed AND staff_user_id IS NOT NULL;
CREATE INDEX ix_checkin_audit ON ${schema_db}.checkin_events(gym_id, occurred_at DESC, allowed);

-- ================================ 08. AUDITORIA TRANSVERSAL
CREATE TABLE ${schema_db}.audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    actor_user_id UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    action TEXT NOT NULL, -- member.disabled, payment.registered, role.assigned, etc.
    entity_type TEXT NOT NULL,
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    request_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ,
    deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);
CREATE INDEX ix_audit_gym_date ON ${schema_db}.audit_events(gym_id, occurred_at DESC);
CREATE INDEX ix_audit_actor_date ON ${schema_db}.audit_events(actor_user_id, occurred_at DESC);

-- Inicializa configuracion y modulos cada vez que el admin registra un gimnasio.
CREATE FUNCTION ${schema_db}.initialize_gym_defaults() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO ${schema_db}.gym_settings(gym_id) VALUES (NEW.id);
    INSERT INTO ${schema_db}.gym_modules(gym_id, module_key, enabled)
    VALUES
        (NEW.id, 'calendar', FALSE),
        (NEW.id, 'routines', TRUE),
        (NEW.id, 'checkin', TRUE),
        (NEW.id, 'pos', TRUE);
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_initialize_gym_defaults
    AFTER INSERT ON ${schema_db}.gym
    FOR EACH ROW EXECUTE FUNCTION ${schema_db}.initialize_gym_defaults();

-- ================================ 09. VISTAS DE CONSULTA
-- Fuente unica de roles efectivos (admin global, staff por gym, member por gym).
CREATE VIEW ${schema_db}.v_user_roles AS
    SELECT pa.user_id, NULL::UUID AS gym_id, 'admin'::TEXT AS role, TRUE AS is_active
      FROM ${schema_db}.platform_admins pa
      JOIN ${schema_db}.users u ON u.id = pa.user_id
     WHERE u.status = 'active'
    UNION ALL
    SELECT sr.user_id, sr.gym_id, sr.role, sr.is_active
      FROM ${schema_db}.gym_staff_roles sr
      JOIN ${schema_db}.users u ON u.id = sr.user_id
     WHERE u.status = 'active'
    UNION ALL
    SELECT m.user_id, m.gym_id, 'member'::TEXT AS role, m.is_active
      FROM ${schema_db}.gym_members m
      JOIN ${schema_db}.users u ON u.id = m.user_id
     WHERE u.status = 'active';

-- Compatible con el chequeo actual: vence si next_due_date < hoy en el gimnasio.
-- Es una vista de lectura: las reglas finales se revalidan al registrar cada acceso.
CREATE VIEW ${schema_db}.v_member_access AS
SELECT
    m.id AS member_id,
    m.gym_id,
    m.user_id,
    m.member_code,
    s.next_due_date,
    CASE
        WHEN g.status <> 'active' THEN 'gym_inactive'
        WHEN u.status <> 'active' THEN 'user_inactive'
        WHEN NOT m.is_active THEN 'member_inactive'
        WHEN s.id IS NULL THEN 'no_active_subscription'
        WHEN s.last_payment_date IS NULL THEN 'no_payment'
        WHEN s.next_due_date < (now() AT TIME ZONE g.timezone)::DATE THEN 'expired'
        ELSE 'allowed'
    END AS access_status
FROM ${schema_db}.gym_members m
JOIN ${schema_db}.gym g ON g.id = m.gym_id
JOIN ${schema_db}.users u ON u.id = m.user_id
LEFT JOIN ${schema_db}.member_subscriptions s
    ON s.gym_id = m.gym_id AND s.member_id = m.id AND s.status = 'active';

-- ================================ 10. CATEGORIA FUNCTIONAL DEL CALENDARIO
-- Incluye la categoría functional utilizada por Vue y conserva las otras cuatro.
ALTER TABLE ${schema_db}.class_templates
    DROP CONSTRAINT IF EXISTS class_templates_category_check;
ALTER TABLE ${schema_db}.class_templates
    ADD CONSTRAINT class_templates_category_check
    CHECK (category IN ('normal','functional','special','overlay_special','event'));

ALTER TABLE ${schema_db}.gym_classes
    DROP CONSTRAINT IF EXISTS gym_classes_category_check;
ALTER TABLE ${schema_db}.gym_classes
    ADD CONSTRAINT gym_classes_category_check
    CHECK (category IN ('normal','functional','special','overlay_special','event'));

-- ================================ 11. PUBLICACION DE LANDING
-- Versión pública inmutable; publicación y booleano se modifican en la misma transacción.
CREATE TABLE ${schema_db}.landing_publications (
    gym_id UUID PRIMARY KEY REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    version BIGINT NOT NULL DEFAULT 1 CHECK (version > 0),
    snapshot JSONB NOT NULL,
    draft_checksum CHAR(64) NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL
);

-- ================================ 12. AUDITORIA Y BORRADO LOGICO UNIVERSAL
-- Todas las tablas reales del esquema, incluso las de clave compuesta,
-- obtienen auditoría y borrado lógico. No elimina datos históricos.
-- NOTA: cuentas/secretos/tokens nunca se copian al registro audit_events.
DO $migration$
DECLARE t record; column_name text;
BEGIN
  FOR t IN SELECT tablename FROM pg_catalog.pg_tables
           WHERE schemaname='${schema_db}'
             AND tablename NOT IN ('flyway_schema_history','databasechangelog','databasechangeloglock')
  LOOP
    EXECUTE format('ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now()', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT now()', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS deleted_at timestamptz', '${schema_db}', t.tablename);
    FOREACH column_name IN ARRAY ARRAY['created_by','updated_by','deleted_by'] LOOP
      EXECUTE format('ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS %I uuid REFERENCES %I.users(id) ON DELETE SET NULL',
         '${schema_db}', t.tablename, column_name, '${schema_db}');
    END LOOP;
    EXECUTE format('UPDATE %I.%I SET created_at=now() WHERE created_at IS NULL', '${schema_db}', t.tablename);
    EXECUTE format('UPDATE %I.%I SET updated_at=now() WHERE updated_at IS NULL', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN created_at SET DEFAULT now()', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN updated_at SET DEFAULT now()', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN created_at SET NOT NULL', '${schema_db}', t.tablename);
    EXECUTE format('ALTER TABLE %I.%I ALTER COLUMN updated_at SET NOT NULL', '${schema_db}', t.tablename);
    EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I.%I (deleted_at) WHERE deleted_at IS NULL',
       'ix_'||t.tablename||'_live', '${schema_db}', t.tablename);
  END LOOP;
END $migration$;

ALTER TABLE ${schema_db}.audit_events ADD COLUMN IF NOT EXISTS entity_key jsonb;

-- app.actor_id se establece con set_config(..., true) en la MISMA transacción JDBC/JPA.
-- Sin actor de sesión (migraciones/bootstrap) los *_by permanecen NULL, nunca se inventan UUID.
CREATE OR REPLACE FUNCTION ${schema_db}.row_audit_metadata() RETURNS trigger LANGUAGE plpgsql AS $fn$
DECLARE actor_id uuid;
BEGIN
  actor_id := NULLIF(current_setting('app.actor_id',true),'')::uuid;
  IF TG_OP='INSERT' THEN
    NEW.created_at := COALESCE(NEW.created_at,now());
    NEW.updated_at := COALESCE(NEW.updated_at,NEW.created_at);
    NEW.created_by := COALESCE(NEW.created_by,actor_id);
    NEW.updated_by := COALESCE(actor_id,NEW.updated_by);
  ELSE
    NEW.created_at := OLD.created_at;
    NEW.created_by := OLD.created_by;
    NEW.updated_at := now();
    NEW.updated_by := COALESCE(actor_id,
      CASE WHEN NEW.updated_by IS DISTINCT FROM OLD.updated_by THEN NEW.updated_by ELSE NULL END);
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
      NEW.deleted_by := COALESCE(actor_id,
        CASE WHEN NEW.deleted_by IS DISTINCT FROM OLD.deleted_by THEN NEW.deleted_by ELSE NULL END);
    ELSIF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN
      NEW.deleted_by := NULL; -- Restauración explícita, por ejemplo al reasignar un rol.
    END IF;
  END IF;
  RETURN NEW;
END $fn$;

CREATE OR REPLACE FUNCTION ${schema_db}.row_audit_event() RETURNS trigger LANGUAGE plpgsql AS $fn$
DECLARE row_data jsonb; actor_id uuid; gym uuid; eid uuid; pk_name text; key_data jsonb := '{}'::jsonb; operation text;
BEGIN
  IF TG_TABLE_NAME='audit_events' THEN RETURN NEW; END IF; -- Evita recursión.
  row_data := to_jsonb(NEW);
  actor_id := NULLIF(current_setting('app.actor_id',true),'')::uuid;
  gym := NULLIF(row_data->>'gym_id','')::uuid;
  IF gym IS NULL AND TG_TABLE_NAME='gym' THEN gym := (row_data->>'id')::uuid; END IF;
  eid := NULLIF(row_data->>'id','')::uuid;
  FOR pk_name IN
    SELECT a.attname FROM pg_index i
    JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=ANY(i.indkey)
    WHERE i.indrelid=TG_RELID AND i.indisprimary
  LOOP
    key_data := key_data || jsonb_build_object(pk_name,row_data->pk_name);
  END LOOP;
  IF TG_OP='INSERT' THEN operation:='created';
  ELSIF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN operation:='deleted';
  ELSIF OLD.deleted_at IS NOT NULL AND NEW.deleted_at IS NULL THEN operation:='restored';
  ELSE operation:='updated'; END IF;
  INSERT INTO ${schema_db}.audit_events(gym_id,actor_user_id,action,entity_type,entity_id,entity_key)
    VALUES (gym,actor_id,operation,TG_TABLE_NAME,eid,key_data);
  RETURN NEW;
END $fn$;

CREATE OR REPLACE FUNCTION ${schema_db}.block_physical_delete() RETURNS trigger LANGUAGE plpgsql AS $fn$
BEGIN
  RAISE EXCEPTION 'Borrado físico deshabilitado para %.%: use deleted_at',TG_TABLE_SCHEMA,TG_TABLE_NAME
    USING ERRCODE='23514';
END $fn$;

DO $migration$
DECLARE t record;
BEGIN
 FOR t IN SELECT tablename FROM pg_catalog.pg_tables
          WHERE schemaname='${schema_db}'
            AND tablename NOT IN ('flyway_schema_history','databasechangelog','databasechangeloglock')
 LOOP
  EXECUTE format('CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON %I.%I FOR EACH ROW EXECUTE FUNCTION %I.row_audit_metadata()', '${schema_db}',t.tablename,'${schema_db}');
  IF t.tablename <> 'audit_events' THEN
    EXECUTE format('CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON %I.%I FOR EACH ROW EXECUTE FUNCTION %I.row_audit_event()', '${schema_db}',t.tablename,'${schema_db}');
  END IF;
  EXECUTE format('CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON %I.%I FOR EACH ROW EXECUTE FUNCTION %I.block_physical_delete()', '${schema_db}',t.tablename,'${schema_db}');
 END LOOP;
END $migration$;

-- Las vistas compartidas no deben otorgar acceso por membresías, roles o sesiones eliminadas.
CREATE OR REPLACE VIEW ${schema_db}.v_user_roles AS
 SELECT pa.user_id,NULL::uuid AS gym_id,'admin'::text AS role,TRUE AS is_active
   FROM ${schema_db}.platform_admins pa JOIN ${schema_db}.users u ON u.id=pa.user_id
  WHERE u.status='active' AND u.deleted_at IS NULL AND pa.deleted_at IS NULL
 UNION ALL
 SELECT sr.user_id,sr.gym_id,sr.role,sr.is_active
   FROM ${schema_db}.gym_staff_roles sr JOIN ${schema_db}.users u ON u.id=sr.user_id
   JOIN ${schema_db}.gym g ON g.id=sr.gym_id
  WHERE u.status='active' AND u.deleted_at IS NULL AND sr.deleted_at IS NULL AND g.deleted_at IS NULL
 UNION ALL
 SELECT m.user_id,m.gym_id,'member'::text,m.is_active
   FROM ${schema_db}.gym_members m JOIN ${schema_db}.users u ON u.id=m.user_id
   JOIN ${schema_db}.gym g ON g.id=m.gym_id
  WHERE u.status='active' AND u.deleted_at IS NULL AND m.deleted_at IS NULL AND g.deleted_at IS NULL;

CREATE OR REPLACE VIEW ${schema_db}.v_member_access AS
SELECT m.id AS member_id,m.gym_id,m.user_id,m.member_code,s.next_due_date,
 CASE WHEN g.status<>'active' THEN 'gym_inactive'
      WHEN u.status<>'active' THEN 'user_inactive'
      WHEN NOT m.is_active THEN 'member_inactive'
      WHEN s.id IS NULL THEN 'no_active_subscription'
      WHEN s.last_payment_date IS NULL THEN 'no_payment'
      WHEN s.next_due_date<(now() AT TIME ZONE g.timezone)::date THEN 'expired'
      ELSE 'allowed' END AS access_status
FROM ${schema_db}.gym_members m
JOIN ${schema_db}.gym g ON g.id=m.gym_id AND g.deleted_at IS NULL
JOIN ${schema_db}.users u ON u.id=m.user_id AND u.deleted_at IS NULL
LEFT JOIN ${schema_db}.member_subscriptions s
  ON s.gym_id=m.gym_id AND s.member_id=m.id AND s.status='active' AND s.deleted_at IS NULL
WHERE m.deleted_at IS NULL;

-- Las claves técnicas (id y claves referenciadas por FK) permanecen únicas.
-- Solo las claves de negocio no referenciadas pasan a únicas ENTRE REGISTROS VIGENTES.
ALTER TABLE ${schema_db}.routine_days DROP CONSTRAINT IF EXISTS routine_days_gym_id_routine_id_day_order_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_routine_days_order
  ON ${schema_db}.routine_days(gym_id,routine_id,day_order) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.routine_exercises DROP CONSTRAINT IF EXISTS routine_exercises_gym_id_routine_day_id_exercise_order_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_routine_exercises_order
  ON ${schema_db}.routine_exercises(gym_id,routine_day_id,exercise_order) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.membership_plans DROP CONSTRAINT IF EXISTS membership_plans_gym_id_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_membership_plan_name
  ON ${schema_db}.membership_plans(gym_id,name) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.gym_members DROP CONSTRAINT IF EXISTS gym_members_gym_id_user_id_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_member_user
  ON ${schema_db}.gym_members(gym_id,user_id) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.gym_members DROP CONSTRAINT IF EXISTS gym_members_gym_id_member_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_member_code
  ON ${schema_db}.gym_members(gym_id,member_code) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.products DROP CONSTRAINT IF EXISTS products_gym_id_sku_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_products_sku
  ON ${schema_db}.products(gym_id,sku) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.checkin_devices DROP CONSTRAINT IF EXISTS checkin_devices_gym_id_device_name_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_device_name
  ON ${schema_db}.checkin_devices(gym_id,device_name) WHERE deleted_at IS NULL;
ALTER TABLE ${schema_db}.guest_passes DROP CONSTRAINT IF EXISTS guest_passes_gym_id_phone_normalized_key;
CREATE UNIQUE INDEX IF NOT EXISTS ux_live_guest_phone
  ON ${schema_db}.guest_passes(gym_id,phone_normalized) WHERE deleted_at IS NULL;

DROP INDEX IF EXISTS ${schema_db}.ux_product_barcode;
CREATE UNIQUE INDEX ux_product_barcode ON ${schema_db}.products(gym_id,barcode)
  WHERE barcode IS NOT NULL AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_one_active_subscription;
CREATE UNIQUE INDEX ux_one_active_subscription ON ${schema_db}.member_subscriptions(gym_id,member_id)
  WHERE status='active' AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_active_reservation_user;
CREATE UNIQUE INDEX ux_active_reservation_user ON ${schema_db}.class_reservations(gym_id,class_id,user_id)
  WHERE user_id IS NOT NULL AND status IN ('booked','attended') AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_active_reservation_guest;
CREATE UNIQUE INDEX ux_active_reservation_guest ON ${schema_db}.class_reservations(gym_id,class_id,guest_pass_id)
  WHERE guest_pass_id IS NOT NULL AND status IN ('booked','attended') AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_member_daily_entry;
CREATE UNIQUE INDEX ux_member_daily_entry ON ${schema_db}.checkin_events(gym_id,member_id,visit_date)
  WHERE allowed AND member_id IS NOT NULL AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_guest_daily_entry;
CREATE UNIQUE INDEX ux_guest_daily_entry ON ${schema_db}.checkin_events(gym_id,guest_pass_id,visit_date)
  WHERE allowed AND guest_pass_id IS NOT NULL AND deleted_at IS NULL;
DROP INDEX IF EXISTS ${schema_db}.ux_staff_daily_entry;
CREATE UNIQUE INDEX ux_staff_daily_entry ON ${schema_db}.checkin_events(gym_id,staff_user_id,visit_date)
  WHERE allowed AND staff_user_id IS NOT NULL AND deleted_at IS NULL;

-- ================================ 13. CHECK-IN: SEGURIDAD DE DISPOSITIVOS E IDEMPOTENCIA
ALTER TABLE ${schema_db}.checkin_devices
  ADD COLUMN activation_expires_at TIMESTAMPTZ,
  ADD COLUMN activated_at TIMESTAMPTZ;

ALTER TABLE ${schema_db}.checkin_events
  ADD COLUMN idempotency_key_hash VARCHAR(64),
  ADD COLUMN request_fingerprint VARCHAR(64);
CREATE UNIQUE INDEX ux_live_checkin_idempotency
 ON ${schema_db}.checkin_events(gym_id,idempotency_key_hash)
 WHERE idempotency_key_hash IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE ${schema_db}.checkin_device_sessions (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 gym_id UUID NOT NULL,
 device_id UUID NOT NULL,
 session_token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL,
 revoked_at TIMESTAMPTZ,
 last_seen_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID,
 FOREIGN KEY (gym_id,device_id) REFERENCES ${schema_db}.checkin_devices(gym_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_kiosk_session_expiration CHECK (expires_at > created_at)
);
CREATE INDEX ix_checkin_device_sessions_live ON ${schema_db}.checkin_device_sessions(device_id,expires_at)
 WHERE deleted_at IS NULL AND revoked_at IS NULL;

-- Registrar nueva tabla en el mecanismo transversal de auditoría y bloqueo de DELETE.
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_device_sessions
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();

-- Una fecha de vencimiento ausente NO da acceso, incluso si existe fecha de pago.
-- Mantener los mismos nombres de campos y prioridades para el módulo manual existente.
CREATE OR REPLACE VIEW ${schema_db}.v_member_access AS
 SELECT m.id AS member_id,m.gym_id,m.user_id,m.member_code,s.next_due_date,
 CASE WHEN g.status<>'active' THEN 'gym_inactive'
      WHEN u.status<>'active' THEN 'user_inactive'
      WHEN NOT m.is_active THEN 'member_inactive'
      WHEN s.id IS NULL THEN 'no_active_subscription'
      WHEN s.last_payment_date IS NULL THEN 'no_payment'
      WHEN s.next_due_date IS NULL THEN 'unknown_validity'
      WHEN s.next_due_date<(now() AT TIME ZONE g.timezone)::date THEN 'expired'
      ELSE 'allowed' END AS access_status
 FROM ${schema_db}.gym_members m
 JOIN ${schema_db}.gym g ON g.id=m.gym_id AND g.deleted_at IS NULL
 JOIN ${schema_db}.users u ON u.id=m.user_id AND u.deleted_at IS NULL
 LEFT JOIN ${schema_db}.member_subscriptions s
   ON s.gym_id=m.gym_id AND s.member_id=m.id AND s.status='active' AND s.deleted_at IS NULL
 WHERE m.deleted_at IS NULL;

-- Límite de abuso compartido entre instancias para intentos de activación.
CREATE TABLE ${schema_db}.checkin_activation_attempts (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 client_hash VARCHAR(64) NOT NULL,
 tried_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID
);
CREATE INDEX ix_checkin_activation_attempts_client ON ${schema_db}.checkin_activation_attempts(client_hash,tried_at)
 WHERE deleted_at IS NULL;
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_activation_attempts
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();

-- ================================ 14. CHECK-IN: CODIGO MANUAL DE UN SOLO USO
-- Código manual temporal, emitido solo al miembro autenticado y canjeado por kiosco autorizado.
CREATE TABLE ${schema_db}.checkin_access_codes (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 gym_id UUID NOT NULL,
 member_id UUID NOT NULL,
 code_hash VARCHAR(64) NOT NULL UNIQUE,
 issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 expires_at TIMESTAMPTZ NOT NULL,
 consumed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 deleted_at TIMESTAMPTZ,
 deleted_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
 FOREIGN KEY (gym_id,member_id) REFERENCES ${schema_db}.gym_members(gym_id,id) ON DELETE RESTRICT,
 CONSTRAINT ck_checkin_manual_code_expiration CHECK (expires_at > issued_at)
);
CREATE INDEX ix_checkin_access_codes_member ON ${schema_db}.checkin_access_codes(gym_id,member_id,issued_at DESC)
 WHERE deleted_at IS NULL;
CREATE INDEX ix_checkin_access_codes_live ON ${schema_db}.checkin_access_codes(gym_id,expires_at)
 WHERE deleted_at IS NULL AND consumed_at IS NULL AND revoked_at IS NULL;
CREATE TRIGGER trg_audit_metadata BEFORE INSERT OR UPDATE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_metadata();
CREATE TRIGGER trg_audit_event AFTER INSERT OR UPDATE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.row_audit_event();
CREATE TRIGGER trg_block_physical_delete BEFORE DELETE ON ${schema_db}.checkin_access_codes
 FOR EACH ROW EXECUTE FUNCTION ${schema_db}.block_physical_delete();
-- El método 'kiosk_code' y el índice único de accesos diarios ya existen desde el baseline.
