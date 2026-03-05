CREATE TABLE IF NOT EXISTS user_watchlist (
    id SERIAL PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(chat_id, ticker)
);

CREATE TABLE IF NOT EXISTS user_alerts (
    id SERIAL PRIMARY KEY,
    chat_id VARCHAR(50) NOT NULL,
    ticker VARCHAR(20) NOT NULL,
    indicator_name VARCHAR(50) NOT NULL,
    condition_operator VARCHAR(10) NOT NULL, -- 'UPPER', 'LOWER'
    target_value NUMERIC NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
