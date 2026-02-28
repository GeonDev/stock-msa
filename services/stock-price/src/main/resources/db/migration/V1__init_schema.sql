CREATE TABLE TB_STOCK_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(50),
    market_code VARCHAR(50),
    bas_dt DATE,
    volume DECIMAL(25, 4),
    volume_price DECIMAL(25, 4),
    start_price DECIMAL(25, 4),
    end_price DECIMAL(25, 4),
    high_price DECIMAL(25, 4),
    low_price DECIMAL(25, 4),
    adj_close_price DECIMAL(25, 4),
    daily_range DECIMAL(25, 4),
    daily_ratio DECIMAL(25, 4),
    stock_total_cnt DECIMAL(25, 4),
    market_total_amt DECIMAL(25, 4),
    market_cap_rank INTEGER,
    market_cap_percentile DECIMAL(5, 2)
);

CREATE TABLE TB_STOCK_INDICATOR (
    stock_price_id BIGINT PRIMARY KEY,
    ma5 DECIMAL(25, 4),
    ma20 DECIMAL(25, 4),
    ma60 DECIMAL(25, 4),
    ma120 DECIMAL(25, 4),
    ma200 DECIMAL(25, 4),
    ma250 DECIMAL(25, 4),
    momentum1m DECIMAL(25, 4),
    momentum3m DECIMAL(25, 4),
    momentum6m DECIMAL(25, 4),
    rsi14 DECIMAL(25, 4),
    bollinger_upper DECIMAL(25, 4),
    bollinger_lower DECIMAL(25, 4),
    macd DECIMAL(25, 4),
    macd_signal DECIMAL(25, 4)
);

CREATE TABLE TB_STOCK_WEEKLY_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(50),
    market_code VARCHAR(50),
    start_date DATE,
    end_date DATE,
    volume DECIMAL(25, 4),
    volume_price DECIMAL(25, 4),
    start_price DECIMAL(25, 4),
    end_price DECIMAL(25, 4),
    high_price DECIMAL(25, 4),
    low_price DECIMAL(25, 4),
    stock_total_cnt DECIMAL(25, 4),
    market_total_amt DECIMAL(25, 4)
);

CREATE TABLE TB_STOCK_MONTHLY_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(50),
    market_code VARCHAR(50),
    start_date DATE,
    end_date DATE,
    volume DECIMAL(25, 4),
    volume_price DECIMAL(25, 4),
    start_price DECIMAL(25, 4),
    end_price DECIMAL(25, 4),
    high_price DECIMAL(25, 4),
    low_price DECIMAL(25, 4),
    stock_total_cnt DECIMAL(25, 4),
    market_total_amt DECIMAL(25, 4)
);

CREATE TABLE TB_CORP_EVENT_HISTORY (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(50),
    event_date VARCHAR(8),
    event_type VARCHAR(50),
    ratio DECIMAL(25, 4),
    amount DECIMAL(25, 4),
    description VARCHAR(255)
);
