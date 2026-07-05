ALTER TABLE events
    ADD COLUMN participant_count INTEGER NOT NULL DEFAULT 0;

UPDATE events e
SET participant_count = (
    SELECT COUNT(*)
    FROM event_participants ep
    WHERE ep.event_id = e.id
);