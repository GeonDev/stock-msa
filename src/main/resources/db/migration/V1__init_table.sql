CREATE TABLE TB_CORP_DETAIL (
    corp_code VARCHAR(255) PRIMARY KEY,
    national VARCHAR(255),
    state VARCHAR(255),
    corp_type VARCHAR(255)
);

CREATE TABLE TB_CORP_FINANCE (
    corp_code VARCHAR(255) PRIMARY KEY,
    bas_dt DATE,
    biz_year VARCHAR(255),
    currency VARCHAR(255),
    op_income INT,
    investment INT,
    net_income INT,
    revenue INT,
    total_asset INT,
    total_debt INT,
    total_capital INT,
    doc_code VARCHAR(255),
    doc_name VARCHAR(255),
    doc_debt_ratio DOUBLE,
    income_before_tax INT
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
    market_total_amt BIGINT,
    momentum INT
);
