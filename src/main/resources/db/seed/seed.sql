--liquibase formatted sql

-- Datos iniciales idempotentes de una instalación nueva.
-- La cuenta administradora se inicializa en el changeset adicional
-- seed_platform_admin.sql. AdminBootstrap puede complementarla con variables
-- de entorno sin sobrescribir una contraseña ya existente.

--changeset gms:seed-001

-- Sucursal de demostración. El trigger initialize_branch_defaults crea su
-- configuración y módulos operativos.
INSERT INTO ${schema_db}.branch (
    id, name, short_name, code, address, phone, timezone, currency, status
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Sucursal Demo',
    'Demo',
    'sucursal-demo',
    'Av. Principal 100',
    '+52 55 0000 0000',
    'America/Mexico_City',
    'MXN',
    'active'
) ON CONFLICT DO NOTHING;

-- Configuración única del sitio (marca). No pertenece a ninguna sucursal.
INSERT INTO ${schema_db}.site_settings (
    id, site_name, short_name, contact_email, contact_phone, address,
    timezone, currency, theme_preset, theme_overrides
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'GMS Fitness',
    'GMS',
    'hola@gms.local',
    '+52 55 0000 0000',
    'Av. Principal 100',
    'America/Mexico_City',
    'MXN',
    'crossfit-dark',
    '{}'::jsonb
) ON CONFLICT (id) DO NOTHING;

-- Imágenes base del sitio. Son URLs HTTPS públicas, no archivos privados.
INSERT INTO ${schema_db}.site_media_assets (
    id, name, media_type, external_url, mime_type
) VALUES
    ('22222222-2222-2222-2222-222222222221', 'Hero entrenamiento', 'image',
     'https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=1920', 'image/jpeg'),
    ('22222222-2222-2222-2222-222222222222', 'Interior del gimnasio', 'image',
     'https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=1920', 'image/jpeg'),
    ('22222222-2222-2222-2222-222222222223', 'Entrenamiento funcional', 'image',
     'https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?q=80&w=1920', 'image/jpeg')
ON CONFLICT (id) DO NOTHING;

-- Contenido inicial de la landing única del sitio.
INSERT INTO ${schema_db}.site_landing_blocks (
    id, block_type, sort_order, enabled, block_data
) VALUES
    ('33333333-3333-3333-3333-333333333331', 'hero', 0, TRUE,
     '{"title":"Tu energía. Tu progreso.","subtitle":"Entrena en un espacio pensado para ti. Descubre nuestros planes, horarios y beneficios.","slides":["22222222-2222-2222-2222-222222222221","22222222-2222-2222-2222-222222222222","22222222-2222-2222-2222-222222222223"]}'::jsonb),
    ('33333333-3333-3333-3333-333333333332', 'text', 1, TRUE,
     '{"subtitle":"Sobre nosotros","title":"Un lugar para superarte","text":"Somos una comunidad que entrena con propósito. Nuestro equipo y nuestras instalaciones están listos para acompañarte en cada etapa.","align":"center","maxWidth":"lg","background":false}'::jsonb),
    ('33333333-3333-3333-3333-333333333333', 'membership', 2, TRUE,
     '{"title":"Planes","subtitle":"Elige el plan que mejor se adapte a ti.","plans":[{"id":"basico","name":"Básico","price":"$499 / mes","featured":false,"benefits":["Acceso al área de pesas","Horario libre","Evaluación inicial"]},{"id":"plus","name":"Plus","price":"$799 / mes","featured":true,"benefits":["Todo el plan Básico","Clases grupales","Rutina personalizada"]}]}'::jsonb),
    ('33333333-3333-3333-3333-333333333334', 'carousel', 3, TRUE,
     '{"title":"Nuestras instalaciones","align":"center","slides":["22222222-2222-2222-2222-222222222221","22222222-2222-2222-2222-222222222222","22222222-2222-2222-2222-222222222223"]}'::jsonb)
ON CONFLICT (id) DO NOTHING;
