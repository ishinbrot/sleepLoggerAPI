-- Create sleep_log table to track nightly sleep metrics per user
CREATE TABLE sleep_log (
                           id BIGSERIAL PRIMARY KEY,
                           user_id VARCHAR(255) NOT NULL,
                           sleep_date DATE NOT NULL,
                           bedtime TIME NOT NULL,
                           wake_time TIME NOT NULL,
                           total_time_in_bed_minutes INT NOT NULL,
                           morning_feeling VARCHAR(10) NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Ensure the feeling matches the required API functional specifications
                           CONSTRAINT chk_morning_feeling CHECK (morning_feeling IN ('BAD', 'OK', 'GOOD')),

    -- Enforce uniqueness so a user can only log one entry per sleep date
                           CONSTRAINT uq_user_sleep_date UNIQUE (user_id, sleep_date)
);

-- Composite index for fast lookup of a user's latest sleep log and date-range filtering for 30-day averages
CREATE INDEX idx_sleep_log_user_date ON sleep_log (user_id, sleep_date DESC);