CREATE TABLE TB_CORP_DETAIL (
    corp_code VARCHAR(255) PRIMARY KEY,
    national VARCHAR(255),
    state VARCHAR(255),
    corp_type VARCHAR(255)
);

CREATE TABLE TB_CORP_FINANCE (
    corp_code VARCHAR(255),
    bas_dt DATE NOT NULL,
    biz_year VARCHAR(255),
    currency VARCHAR(255),
    op_income BIGINT,
    investment BIGINT,
    net_income BIGINT,
    revenue BIGINT,
    total_asset BIGINT,
    total_debt BIGINT,
    total_capital BIGINT,
    doc_code VARCHAR(255),
    doc_name VARCHAR(255),
    doc_debt_ratio DOUBLE,
    income_before_tax BIGINT,
    prev_op_income BIGINT,
    prev_net_income BIGINT,
    prev_revenue BIGINT,
    PRIMARY KEY (corp_code, bas_dt)
);

CREATE TABLE TB_CORP_INFO (
    corp_code VARCHAR(255) PRIMARY KEY,
    corp_name VARCHAR(255),
    stock_code VARCHAR(255),
    isin_code VARCHAR(255),
    check_dt DATE
);

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

CREATE TABLE TB_CORP_FINANCE_INDICATOR (
    corp_code VARCHAR(255),
    bas_dt DATE NOT NULL,
    per DOUBLE,
    pbr DOUBLE,
    psr DOUBLE,
    revenue_growth DOUBLE,
    net_income_growth DOUBLE,
    op_income_growth DOUBLE,
    roe DOUBLE,
    roa DOUBLE,
    debt_ratio DOUBLE,
    PRIMARY KEY (corp_code, bas_dt)
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