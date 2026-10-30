-- Smaller logo mark: 320px WebP (~40 KB) instead of the 468px PNG (~257 KB).
UPDATE tenant
SET branding = branding || '{"logoMark": "/assets/miel-spa/logo-mark.webp"}'::jsonb
WHERE slug = 'miel-spa';
