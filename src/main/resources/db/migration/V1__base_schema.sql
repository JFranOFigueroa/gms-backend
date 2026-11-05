-- GMS | Propuesta de esquema relacional v1.0
-- Origen: frontend Vue/Pinia contenido en src.zip (03/11/2026)
-- Motor objetivo: PostgreSQL 15+ | Schema: test
-- ADVERTENCIA: Esquema inicial. La API Java debe comprobar permisos por gimnasio;
-- no conectar el frontend directamente a esta base de datos.
-- Esta migracion NO crea cuentas ni contrasenas de demostracion.

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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE ${schema_db}.users
    ADD CONSTRAINT fk_users_default_gym FOREIGN KEY (default_gym_id)
    REFERENCES ${schema_db}.gym(id) ON DELETE SET NULL;

-- Rol GLOBAL de soporte/administracion tecnica. No se asigna por gimnasio.
CREATE TABLE ${schema_db}.platform_admins (
    user_id UUID PRIMARY KEY REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    assigned_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Un mismo usuario puede ser owner de varios gimnasios o trainer en otros.
CREATE TABLE ${schema_db}.gym_staff_roles (
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES ${schema_db}.users(id) ON DELETE RESTRICT,
    role TEXT NOT NULL CHECK (role IN ('owner','trainer')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Solo modulos configurables. users/members/gym son modulos estructurales.
CREATE TABLE ${schema_db}.gym_modules (
    gym_id UUID NOT NULL REFERENCES ${schema_db}.gym(id) ON DELETE CASCADE,
    module_key TEXT NOT NULL CHECK (module_key IN ('calendar','routines','checkin','pos')),
    enabled BOOLEAN NOT NULL,
    options JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_by UUID REFERENCES ${schema_db}.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (gym_id, id)
);
CREATE INDEX ix_routines_published ON ${schema_db}.routines(gym_id, published);

CREATE TABLE ${schema_db}.routine_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gym_id UUID NOT NULL,
    routine_id UUID NOT NULL,
    day_order INTEGER NOT NULL CHECK (day_order >= 1),
    day_label TEXT NOT NULL,
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
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
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
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

