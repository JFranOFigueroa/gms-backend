-- V4: Se preservan V1-V3. Todas las tablas reales del esquema, incluso las de clave compuesta,
-- obtienen auditoría y borrado lógico. No elimina datos históricos.
-- NOTA: cuentas/secretos/tokens nunca se copian al registro audit_events.
DO $migration$
DECLARE t record; column_name text;
BEGIN
  FOR t IN SELECT tablename FROM pg_catalog.pg_tables
           WHERE schemaname='${schema_db}' AND tablename <> 'flyway_schema_history'
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
          WHERE schemaname='${schema_db}' AND tablename<>'flyway_schema_history'
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
