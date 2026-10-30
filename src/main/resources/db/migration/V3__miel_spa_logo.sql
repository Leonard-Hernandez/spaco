-- Miel Spa's logo mark, served by spaco-app from public/assets/miel-spa.
UPDATE tenant
SET branding = branding || '{"logoMark": "/assets/miel-spa/logo-mark.png"}'::jsonb
WHERE slug = 'miel-spa';
