-- Frameless emblem with a transparent background, under a new file name so the
-- browser-cached opaque version (Cache-Control max-age=86400) is bypassed.
UPDATE tenant
SET branding = branding || '{"logoMark": "/assets/miel-spa/logo-emblem-v2.webp"}'::jsonb
WHERE slug = 'miel-spa';
