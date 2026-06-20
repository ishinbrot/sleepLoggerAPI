UPDATE sleep_log
SET user_id = LEFT(user_id, 20)
WHERE LENGTH(user_id) > 20;

ALTER TABLE sleep_log
ALTER COLUMN user_id TYPE VARCHAR(20);

ANALYZE sleep_log;