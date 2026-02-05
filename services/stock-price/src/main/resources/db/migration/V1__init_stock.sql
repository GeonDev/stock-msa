CREATE TABLE TB_STOCK_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(255),
    market_code VARCHAR(255),
    bas_dt DATE,
    volume INT,
    volume_price BIGINT,
    start_price INT,
    end_price INT,
    high_price INT,
    low_price INT,
    daily_range DOUBLE,
    daily_ratio DOUBLE,
    stock_total_cnt BIGINT,
    market_total_amt BIGINT
);

CREATE TABLE TB_STOCK_INDICATOR (
    stock_price_id BIGINT PRIMARY KEY,
    ma5 DOUBLE,
    ma20 DOUBLE,
    ma60 DOUBLE,
    ma120 DOUBLE,
    ma200 DOUBLE,
    ma250 DOUBLE,
    momentum1m DOUBLE,
    momentum3m DOUBLE,
    momentum6m DOUBLE
);

CREATE TABLE TB_STOCK_WEEKLY_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(255),
    market_code VARCHAR(255),
    start_date DATE,
    end_date DATE,
    volume INT,
    volume_price BIGINT,
    start_price INT,
    end_price INT,
    high_price INT,
    low_price INT,
    stock_total_cnt BIGINT,
    market_total_amt BIGINT
);

CREATE TABLE TB_STOCK_MONTHLY_PRICE (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(255),
    market_code VARCHAR(255),
    start_date DATE,
    end_date DATE,
    volume INT,
    volume_price BIGINT,
    start_price INT,
    end_price INT,
    high_price INT,
    low_price INT,
    stock_total_cnt BIGINT,
    market_total_amt BIGINT
);
