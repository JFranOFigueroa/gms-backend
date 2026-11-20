--liquibase formatted sql

-- Liquibase: cambio incremental en suscripciones y pagos, sin pérdida de históricos.
--changeset gms:payments-admin-plans-001 dbms:postgresql
CREATE UNIQUE INDEX IF NOT EXISTS ux_branch_admin_plan_branch_id_id
  ON ${schema_db}.branch_admin_plans (branch_id, id);

ALTER TABLE ${schema_db}.member_subscriptions
  ALTER COLUMN plan_id DROP NOT NULL,
  ADD COLUMN admin_plan_id UUID;
ALTER TABLE ${schema_db}.member_subscriptions
  ADD CONSTRAINT fk_subscription_admin_plan_branch
    FOREIGN KEY (branch_id, admin_plan_id)
    REFERENCES ${schema_db}.branch_admin_plans (branch_id, id) ON DELETE RESTRICT,
  ADD CONSTRAINT ck_subscription_exactly_one_plan
    CHECK (num_nonnulls(plan_id, admin_plan_id) = 1);

ALTER TABLE ${schema_db}.membership_payments
  ADD COLUMN plan_id UUID,
  ADD COLUMN admin_plan_id UUID;
ALTER TABLE ${schema_db}.membership_payments
  ADD CONSTRAINT fk_payment_legacy_plan_branch
    FOREIGN KEY (branch_id, plan_id)
    REFERENCES ${schema_db}.membership_plans (branch_id, id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_payment_admin_plan_branch
    FOREIGN KEY (branch_id, admin_plan_id)
    REFERENCES ${schema_db}.branch_admin_plans (branch_id, id) ON DELETE RESTRICT,
  ADD CONSTRAINT ck_payment_at_most_one_catalog_plan
    CHECK (num_nonnulls(plan_id, admin_plan_id) <= 1);

ALTER TABLE ${schema_db}.membership_payments
  DROP CONSTRAINT IF EXISTS membership_payments_periodicity_check;
ALTER TABLE ${schema_db}.membership_payments
  ADD CONSTRAINT ck_payment_periodicity_catalog
    CHECK (periodicity IN ('monthly', 'yearly', 'daily'));

CREATE INDEX IF NOT EXISTS ix_subscription_admin_plan
  ON ${schema_db}.member_subscriptions (branch_id, admin_plan_id)
  WHERE admin_plan_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_payment_admin_plan
  ON ${schema_db}.membership_payments (branch_id, admin_plan_id)
  WHERE admin_plan_id IS NOT NULL;
