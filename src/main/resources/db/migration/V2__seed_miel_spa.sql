-- Demo tenant: Miel Spa & Bienestar (Cali). Catalogue from miel-spa-design/PRODUCT.md.
-- Facts the spa has not confirmed stay as placeholders ([DIRECCIÓN], [HORARIO], [NÚMERO],
-- [PROFESIONAL n]) and prices stay NULL (the site shows "Consultar"): never invent them.

INSERT INTO tenant (slug, name, timezone, contact_phone, address, slot_step_min, min_booking_lead_min, branding)
VALUES ('miel-spa', 'Miel Spa & Bienestar', 'America/Bogota', NULL, '[DIRECCIÓN]', 30, 120,
        '{
          "wordmark": "miel",
          "subline": "SPA & BIENESTAR",
          "tagline": "Donde la dulzura de la naturaleza te renueva",
          "intro": "Masajes, faciales y rituales con miel, lavanda y botánicos. Elige tu tratamiento y agenda tu cita en menos de un minuto.",
          "city": "Cali",
          "hours": "[HORARIO]",
          "address": "[DIRECCIÓN]",
          "whatsapp": "[NÚMERO]",
          "email": "[CORREO]",
          "instagram": "[@INSTAGRAM]",
          "heroTreatment": "ritual-miel",
          "cancellationNote": "Si no puedes asistir, cancela desde el enlace de tu cita."
        }'::jsonb);

INSERT INTO treatment_category (tenant_id, name, slug, ink, position)
SELECT t.id, c.name, c.slug, c.ink, c.position
FROM tenant t,
     (VALUES ('Masajes', 'masajes', 'honey', 1),
             ('Faciales', 'faciales', 'lavender', 2),
             ('Corporales', 'corporales', 'sage', 3),
             ('Rituales Miel', 'rituales', 'navy', 4),
             ('Manos y pies', 'manos', 'clay', 5)) AS c(name, slug, ink, position)
WHERE t.slug = 'miel-spa';

INSERT INTO treatment (tenant_id, category_id, formula_no, name, slug, tagline, duration_min, buffer_min, position)
SELECT t.id, c.id, x.no, x.name, x.slug, x.tagline, x.dur, 15, x.no
FROM tenant t
JOIN (VALUES
    (1, 'rituales',   'Ritual Miel', 'ritual-miel', 'Exfoliación, masaje relajante y facial con miel en una sola experiencia.', 120),
    (2, 'masajes',    'Masaje relajante', 'masaje-relajante', 'Presión suave con aceites tibios para soltar la tensión y dormir mejor.', 60),
    (3, 'masajes',    'Masaje con piedras calientes', 'masaje-piedras-calientes', 'El calor de las piedras volcánicas relaja la musculatura profunda.', 75),
    (4, 'masajes',    'Masaje descontracturante', 'masaje-descontracturante', 'Trabajo focalizado en espalda, cuello y hombros cargados.', 60),
    (5, 'faciales',   'Facial hidratante con miel', 'facial-hidratante-miel', 'Limpieza suave, mascarilla de miel y masaje facial para una piel luminosa.', 50),
    (6, 'faciales',   'Limpieza facial profunda', 'limpieza-facial-profunda', 'Exfoliación, extracción e hidratación adaptadas a tu tipo de piel.', 60),
    (7, 'corporales', 'Exfoliación de miel y avena', 'exfoliacion-miel-avena', 'Renueva la piel de todo el cuerpo y la deja suave e hidratada.', 45),
    (8, 'corporales', 'Envoltura de lavanda', 'envoltura-lavanda', 'Envoltura tibia con lavanda que calma el cuerpo y la mente.', 60),
    (9, 'manos',      'Manicure y pedicure spa', 'manicure-pedicure-spa', 'Baño aromático, exfoliación, masaje y esmaltado para manos y pies.', 90)
) AS x(no, cat, name, slug, tagline, dur) ON true
JOIN treatment_category c ON c.tenant_id = t.id AND c.slug = x.cat
WHERE t.slug = 'miel-spa';

INSERT INTO professional (tenant_id, display_name, title)
SELECT t.id, p.name, 'Esteticista'
FROM tenant t, (VALUES ('[PROFESIONAL 1]'), ('[PROFESIONAL 2]')) AS p(name)
WHERE t.slug = 'miel-spa';

-- The time slots of the design (9-11 a. m., 2-6 p. m.), Monday to Saturday, until the
-- owner sets the real hours in the panel
INSERT INTO availability_rule (tenant_id, professional_id, weekday, start_time, end_time)
SELECT p.tenant_id, p.id, d.weekday, w.start_time, w.end_time
FROM professional p
JOIN tenant t ON t.id = p.tenant_id AND t.slug = 'miel-spa',
     generate_series(1, 6) AS d(weekday),
     (VALUES (TIME '09:00', TIME '12:00'), (TIME '14:00', TIME '19:00')) AS w(start_time, end_time);

-- Draft consent texts: the spa must have them reviewed by its legal adviser before going live
INSERT INTO consent_template (tenant_id, type, version, title, body)
SELECT t.id, x.type, 1, x.title, x.body
FROM tenant t,
     (VALUES
        ('DATA_PROCESSING', 'Autorización de tratamiento de datos personales',
         'Autorizo a Miel Spa & Bienestar a tratar mis datos personales, incluidos datos sensibles de salud que registre en mi historia clínica, para agendar y prestar los servicios, hacer seguimiento a mis tratamientos y contactarme por WhatsApp, correo o teléfono, conforme a la Ley 1581 de 2012 y sus decretos reglamentarios. Sé que puedo conocer, actualizar, rectificar y suprimir mis datos, y revocar esta autorización escribiendo al spa. Entiendo que no estoy obligada a entregar datos sensibles.'),
        ('TREATMENT', 'Consentimiento informado del tratamiento',
         'Declaro que la profesional me explicó en qué consiste el tratamiento, sus beneficios, cuidados y posibles reacciones, que informé con veracidad mis antecedentes de salud, alergias, medicamentos y si estoy en embarazo o lactancia, y que acepto realizarlo.'),
        ('PHOTO_TESTIMONIAL', 'Autorización de uso de fotos de evolución',
         'Autorizo a Miel Spa & Bienestar a publicar en su sitio web, sin mi nombre completo, las fotos de antes y después de mis tratamientos que yo o la profesional seleccionemos, junto con mi testimonio. Puedo revocar esta autorización en cualquier momento desde mi historia en el sitio web y las fotos se retirarán de inmediato.')
     ) AS x(type, title, body)
WHERE t.slug = 'miel-spa';
