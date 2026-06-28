CREATE TABLE users
(
    id          BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT      NOT NULL UNIQUE,
    username    VARCHAR(255),
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255),
    image_url   TEXT,
    age         INTEGER      NOT NULL,
    gender      VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE')),
    is_blocked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE events
(
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(35)   NOT NULL,
    description      TEXT,
    category         VARCHAR(50)    NOT NULL CHECK (category IN ('TECHNOLOGY', 'BUSINESS', 'ART', 'SPORT', 'EDUCATION', 'MUSIC', 'FOOD', 'NETWORKING', 'HEALTH', 'OTHER')),
    status           VARCHAR(20)    NOT NULL CHECK (status IN ('ONGOING', 'PUBLISHED', 'CANCELLED', 'FINISHED')) DEFAULT 'PUBLISHED',
    organizer_id     BIGINT         NOT NULL REFERENCES users (id),
    address          VARCHAR(255),
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    starts_at        TIMESTAMP      NOT NULL,
    ends_at          TIMESTAMP,
    max_participants INTEGER,
    price            NUMERIC(10, 2),
    image_url        TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE event_participants
(
    event_id       BIGINT NOT NULL REFERENCES events (id),
    participant_id BIGINT NOT NULL REFERENCES users (id),
    PRIMARY KEY (event_id, participant_id)
);

CREATE INDEX idx_events_organizer_id ON events (organizer_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_category ON events (category);
CREATE INDEX idx_events_starts_at ON events (starts_at);
CREATE INDEX idx_event_participants_event_id ON event_participants (event_id);
CREATE INDEX idx_event_participants_participant_id ON event_participants (participant_id);
