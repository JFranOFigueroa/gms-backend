-- Incluye la categoría functional utilizada por Vue y conserva las otras cuatro.
-- No modificar V1: Flyway debe aplicar este cambio a instalaciones existentes.
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
