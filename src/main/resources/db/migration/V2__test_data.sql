INSERT INTO users (telegram_id, username, first_name, last_name, age, gender, is_blocked)
VALUES (100000001, 'alex_dev', 'Александр', 'Петров', 28, 'MALE', FALSE),
       (100000002, 'maria_art', 'Мария', 'Иванова', 24, 'FEMALE', FALSE),
       (100000003, 'nikita_sport', 'Никита', 'Сидоров', 31, 'MALE', FALSE);

INSERT INTO events (title, description, category, status, organizer_id, address, latitude, longitude,
                    starts_at, ends_at, max_participants, price, image_url)
VALUES ('Митап по Spring Boot',
        'Обсудим новинки Spring Boot 3, разберём реальные кейсы и пообщаемся с разработчиками.',
        'TECHNOLOGY', 'PUBLISHED',
        (SELECT id FROM users WHERE telegram_id = 100000001),
        'Москва, ул. Льва Толстого, 16', 55.733974, 37.587093,
        NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days 3 hours',
        50, NULL, NULL),

       ('Йога в парке',
        'Утренняя практика для всех уровней подготовки. Приходите в удобной одежде.',
        'HEALTH', 'PUBLISHED',
        (SELECT id FROM users WHERE telegram_id = 100000002),
        'Москва, Парк Горького', 55.729957, 37.601021,
        NOW() + INTERVAL '1 day', NOW() + INTERVAL '1 day 2 hours',
        20, NULL, NULL),

       ('Открытая тренировка по баскетболу',
        'Дружеская игра, берём всех. Нужны кроссовки и желание побегать.',
        'SPORT', 'PUBLISHED',
        (SELECT id FROM users WHERE telegram_id = 100000003),
        'Москва, Лужники', 55.715809, 37.554827,
        NOW() + INTERVAL '5 days', NOW() + INTERVAL '5 days 2 hours',
        30, 300.00, NULL);

INSERT INTO event_participants (event_id, participant_id)
VALUES ((SELECT id FROM events WHERE title = 'Митап по Spring Boot'),
        (SELECT id FROM users WHERE telegram_id = 100000002)),

       ((SELECT id FROM events WHERE title = 'Митап по Spring Boot'),
        (SELECT id FROM users WHERE telegram_id = 100000003)),

       ((SELECT id FROM events WHERE title = 'Йога в парке'),
        (SELECT id FROM users WHERE telegram_id = 100000001));
