ALTER TABLE quiz_sessions ADD COLUMN active_slot BOOLEAN;

UPDATE quiz_sessions
SET phase = 'FINISHED', active_slot = NULL
WHERE phase <> 'FINISHED'
  AND id <> (
    SELECT latest.id
    FROM (
      SELECT MAX(id) AS id
      FROM quiz_sessions
      WHERE phase <> 'FINISHED'
    ) latest
  );

UPDATE quiz_sessions
SET active_slot = TRUE
WHERE phase <> 'FINISHED';

ALTER TABLE quiz_sessions
ADD CONSTRAINT uk_quiz_sessions_active_slot UNIQUE (active_slot);
