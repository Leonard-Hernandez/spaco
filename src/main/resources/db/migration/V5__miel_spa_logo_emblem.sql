-- New brand identity: emblem logo (320px WebP) under a new file name so cached copies of the old mark are bypassed.
UPDATE tenant
SET branding = branding || '{"logoMark": "/assets/miel-spa/logo-emblem.webp"}'::jsonb
WHERE slug = 'miel-spa';
