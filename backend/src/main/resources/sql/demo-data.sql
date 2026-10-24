-- ═════════════════════════════════════════════════════════════════════════════
-- Données de démonstration (profil "demo" uniquement, cf. application-demo.properties)
--
-- Idempotent : chaque INSERT vérifie que la ligne n'existe pas déjà, le script peut
-- donc être rejoué à chaque démarrage sans créer de doublons.
-- Mot de passe de tous les comptes : Mazlove2026!   (hash BCrypt coût 10 ci-dessous)
-- Positions autour de Paris, volontairement approximatives.
-- ═════════════════════════════════════════════════════════════════════════════

-- Comptes : 2 clients et 4 praticiens
INSERT INTO users (email, name, password, phone, role, rating)
VALUES
    ('client@mazlove.test', 'Camille Martin',  '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 12 34 56 78', 'CLIENT',   0),
    ('hugo@mazlove.test',   'Hugo Bernard',    '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 23 45 67 89', 'CLIENT',   0),
    ('lea@mazlove.test',    'Léa Dubois',      '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 34 56 78 90', 'PROVIDER', 0),
    ('samir@mazlove.test',  'Samir Haddad',    '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 45 67 89 01', 'PROVIDER', 0),
    ('ines@mazlove.test',   'Inès Moreau',     '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 56 78 90 12', 'PROVIDER', 0),
    ('thomas@mazlove.test', 'Thomas Lefèvre',  '$2a$10$mIDYKKK63VshBalKNJj7VuPXXY82iINzvzkiMZavEeyKgKykIHWHa', '06 67 89 01 23', 'PROVIDER', 0)
ON CONFLICT (email) DO NOTHING;

-- Fiches praticiens (une par compte PROVIDER)
INSERT INTO providers (user_id, bio, category, latitude, longitude, verified)
SELECT u.id, v.bio, v.category, v.lat, v.lng, v.verified
FROM (VALUES
    ('lea@mazlove.test',
     'Praticienne certifiée depuis 2021. Je commence chaque séance par un temps d''échange sur vos limites et vos envies, puis nous choisissons ensemble les positions. Vous pouvez faire une pause ou arrêter à tout moment, sans avoir à vous justifier.',
     'Câlins assis et allongés', 48.8592, 2.3791, TRUE),
    ('samir@mazlove.test',
     'Ancien infirmier, je propose des séances calmes et très encadrées, idéales pour une première expérience. Tenue confortable et lieu neutre et lumineux.',
     'Première séance', 48.8867, 2.3431, TRUE),
    ('ines@mazlove.test',
     'Je travaille surtout avec des personnes qui traversent une période de solitude ou de deuil. Séances lentes, beaucoup d''écoute, contact uniquement sur les zones que vous validez.',
     'Accompagnement émotionnel', 48.8474, 2.4392, FALSE),
    ('thomas@mazlove.test',
     'Séances en cuillère ou côte à côte, dans un cabinet dédié à Boulogne. Je parle peu et laisse beaucoup de place au silence si vous le souhaitez.',
     'Cuillère', 48.8353, 2.2410, TRUE)
) AS v(email, bio, category, lat, lng, verified)
JOIN users u ON u.email = v.email
WHERE NOT EXISTS (SELECT 1 FROM providers p WHERE p.user_id = u.id);

-- Créneaux hebdomadaires (format "HH:mm" sur 5 caractères)
INSERT INTO provider_availability (provider_id, day_of_week, start_time, end_time)
SELECT p.id, v.day_of_week, v.start_time, v.end_time
FROM (VALUES
    ('lea@mazlove.test',    'MONDAY',    '10:00', '19:00'),
    ('lea@mazlove.test',    'WEDNESDAY', '10:00', '19:00'),
    ('lea@mazlove.test',    'FRIDAY',    '14:00', '21:00'),
    ('lea@mazlove.test',    'SATURDAY',  '10:00', '18:00'),
    ('samir@mazlove.test',  'TUESDAY',   '09:00', '17:00'),
    ('samir@mazlove.test',  'THURSDAY',  '09:00', '17:00'),
    ('samir@mazlove.test',  'SATURDAY',  '09:00', '13:00'),
    ('ines@mazlove.test',   'MONDAY',    '18:00', '22:00'),
    ('ines@mazlove.test',   'TUESDAY',   '18:00', '22:00'),
    ('ines@mazlove.test',   'SUNDAY',    '10:00', '16:00'),
    ('thomas@mazlove.test', 'WEDNESDAY', '12:00', '20:00'),
    ('thomas@mazlove.test', 'THURSDAY',  '12:00', '20:00'),
    ('thomas@mazlove.test', 'FRIDAY',    '12:00', '20:00'),
    ('thomas@mazlove.test', 'SUNDAY',    '14:00', '19:00')
) AS v(email, day_of_week, start_time, end_time)
JOIN users u ON u.email = v.email
JOIN providers p ON p.user_id = u.id
WHERE NOT EXISTS (
    SELECT 1 FROM provider_availability a
    WHERE a.provider_id = p.id AND a.day_of_week = v.day_of_week AND a.start_time = v.start_time
);

-- Séances passées et terminées, pour que les fiches aient déjà des avis
INSERT INTO sessions (client_id, provider_id, datetime, duration, status, code_validation, notes)
SELECT c.id, p.id, v.starts_at, v.duration, 'DONE', v.code, NULL
FROM (VALUES
    ('client@mazlove.test', 'lea@mazlove.test',    TIMESTAMP '2026-09-12 15:00', 1, 'DEMO01'),
    ('hugo@mazlove.test',   'lea@mazlove.test',    TIMESTAMP '2026-09-19 11:00', 2, 'DEMO02'),
    ('client@mazlove.test', 'samir@mazlove.test',  TIMESTAMP '2026-09-24 10:00', 1, 'DEMO03'),
    ('hugo@mazlove.test',   'thomas@mazlove.test', TIMESTAMP '2026-10-02 14:00', 1, 'DEMO04')
) AS v(client_email, provider_email, starts_at, duration, code)
JOIN users c  ON c.email = v.client_email
JOIN users pu ON pu.email = v.provider_email
JOIN providers p ON p.user_id = pu.id
WHERE NOT EXISTS (
    SELECT 1 FROM sessions s
    WHERE s.client_id = c.id AND s.provider_id = p.id AND s.datetime = v.starts_at
);

-- Avis associés (un par séance, comme l'impose uq_reviews_session)
INSERT INTO reviews (user_id, session_id, provider_id, rating, comment)
SELECT s.client_id, s.id, s.provider_id, v.rating, v.comment
FROM (VALUES
    ('DEMO01', 5, 'Léa a pris le temps de m''expliquer le déroulé et de poser le cadre. Je me suis sentie en sécurité du début à la fin.'),
    ('DEMO02', 5, 'Très respectueuse des limites que j''avais données. J''ai pu demander une pause sans aucune gêne.'),
    ('DEMO03', 4, 'Parfait pour une première fois, Samir explique tout. Lieu un peu difficile à trouver.'),
    ('DEMO04', 5, 'Calme, ponctuel, exactement ce qu''il décrit sur sa fiche.')
) AS v(code, rating, comment)
JOIN sessions s ON s.code_validation = v.code
WHERE NOT EXISTS (SELECT 1 FROM reviews r WHERE r.session_id = s.id);
