
ALTER TABLE sleep_log ALTER COLUMN user_id TYPE VARCHAR(36);

CREATE INDEX idx_user_sleep_date ON sleep_log(user_id, sleep_date);