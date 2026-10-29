-- Jeu de données local uniquement. Tous les comptes utilisent password123.
-- Les INSERT sont rejouables sans changer les comptes ou données préexistants.
-- Le modèle actuel exprime les durées en heures entières : les prestations sont
-- donc de 1 h et 2 h, afin de rester compatibles avec la réservation.
INSERT INTO users (email, name, password, phone, role, rating)
VALUES
    ('alice@test.com',  'Alice',  '$2b$10$abcdefghijklmnopqrstuu0SYq8twpcthS10uxQ26rv0s3Obj8Ufu', '0600000001', 'CLIENT', 0),
    ('bob@test.com',    'Bob',    '$2b$10$abcdefghijklmnopqrstuu0SYq8twpcthS10uxQ26rv0s3Obj8Ufu', '0600000002', 'CLIENT', 0),
    ('sophie@test.com', 'Sophie', '$2b$10$abcdefghijklmnopqrstuu0SYq8twpcthS10uxQ26rv0s3Obj8Ufu', '0600000003', 'PROVIDER', 0),
    ('marc@test.com',   'Marc',   '$2b$10$abcdefghijklmnopqrstuu0SYq8twpcthS10uxQ26rv0s3Obj8Ufu', '0600000004', 'PROVIDER', 0),
    ('emma@test.com',   'Emma',   '$2b$10$abcdefghijklmnopqrstuu0SYq8twpcthS10uxQ26rv0s3Obj8Ufu', '0600000005', 'PROVIDER', 0)
ON CONFLICT (email) DO NOTHING;

INSERT INTO providers (user_id, bio, category, latitude, longitude, verified)
SELECT u.id, v.bio, v.category, v.lat, v.lng, TRUE
FROM (VALUES
    ('sophie@test.com', 'Des séances de câlins pour se détendre dans un cadre bienveillant.', 'relaxation', 48.866, 2.347),
    ('marc@test.com', 'Un accompagnement calme et attentif, adapté à vos limites.', 'therapeutique', 48.876, 2.363),
    ('emma@test.com', 'Un temps de présence, de câlins et de méditation.', 'meditation', 48.851, 2.337)
) AS v(email, bio, category, lat, lng)
JOIN users u ON u.email = v.email AND u.role = 'PROVIDER'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO provider_availability (provider_id, day_of_week, start_time, end_time)
SELECT p.id, v.day, v.start_time, v.end_time
FROM providers p JOIN users u ON u.id = p.user_id
CROSS JOIN (VALUES ('MONDAY', '10:00', '18:00'), ('WEDNESDAY', '14:00', '20:00')) AS v(day, start_time, end_time)
WHERE u.email IN ('sophie@test.com', 'marc@test.com', 'emma@test.com')
AND NOT EXISTS (SELECT 1 FROM provider_availability a WHERE a.provider_id = p.id
    AND a.day_of_week = v.day AND a.start_time = v.start_time AND a.end_time = v.end_time);

INSERT INTO provider_images (provider_id, image_url)
SELECT p.id, 'https://placehold.co/600x400/png'
FROM providers p JOIN users u ON u.id = p.user_id
WHERE u.email IN ('sophie@test.com', 'marc@test.com', 'emma@test.com')
AND NOT EXISTS (SELECT 1 FROM provider_images i WHERE i.provider_id = p.id AND i.image_url = 'https://placehold.co/600x400/png');

INSERT INTO service_items (provider_id, name, description, price, duration)
SELECT p.id, v.name, v.description, v.price, v.duration
FROM providers p JOIN users u ON u.id = p.user_id
CROSS JOIN (VALUES
    ('Câlin 1h', 'Une heure de détente et d’écoute', 89.0, 1),
    ('Câlin 2h', 'Deux heures de détente et d’écoute', 149.0, 2)
) AS v(name, description, price, duration)
WHERE u.email IN ('sophie@test.com', 'marc@test.com', 'emma@test.com')
AND NOT EXISTS (SELECT 1 FROM service_items s WHERE s.provider_id = p.id AND s.name = v.name);

-- Chaque avis correspond à une séance terminée, conformément aux règles métier.
-- Les marqueurs dans notes permettent de rejouer le seed même un autre jour.
INSERT INTO sessions (client_id, provider_id, datetime, duration, status, code_validation, notes)
SELECT c.id, p.id, CURRENT_DATE + v.at_time::time + v.day_offset * INTERVAL '1 day',
       1, v.status, v.code, v.marker
FROM (VALUES
    ('alice@test.com', 'sophie@test.com',  1, '14:00', 'PENDING',  '710001', 'seed:pending-sophie'),
    ('alice@test.com', 'marc@test.com',   -1, '10:00', 'ACCEPTED', '710002', 'seed:accepted-marc'),
    ('bob@test.com',   'emma@test.com',   -3, '10:00', 'DONE',     '710003', 'seed:done-emma'),
    ('alice@test.com', 'sophie@test.com', -5, '10:00', 'DONE',     '710004', 'seed:done-sophie'),
    ('bob@test.com',   'marc@test.com',   -7, '10:00', 'DONE',     '710005', 'seed:done-marc')
) AS v(client_email, provider_email, day_offset, at_time, status, code, marker)
JOIN users c ON c.email = v.client_email
JOIN users pu ON pu.email = v.provider_email
JOIN providers p ON p.user_id = pu.id
WHERE NOT EXISTS (SELECT 1 FROM sessions s WHERE s.client_id = c.id AND s.provider_id = p.id AND s.notes = v.marker);

INSERT INTO reviews (user_id, session_id, provider_id, rating, comment)
SELECT s.client_id, s.id, s.provider_id, v.rating, v.comment
FROM (VALUES
    ('seed:done-sophie', 5, 'Moment merveilleux, très à l''écoute'),
    ('seed:done-marc',   4, 'Très professionnel et bienveillant'),
    ('seed:done-emma',   5, 'Je recommande vivement')
) AS v(marker, rating, comment)
JOIN sessions s ON s.notes = v.marker AND s.status = 'DONE'
WHERE NOT EXISTS (SELECT 1 FROM reviews r WHERE r.session_id = s.id)
ON CONFLICT (session_id) DO NOTHING;
