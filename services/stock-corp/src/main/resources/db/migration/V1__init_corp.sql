CREATE TABLE TB_CORP_INFO (
    corp_code VARCHAR(255) PRIMARY KEY,
    corp_name VARCHAR(255),
    stock_code VARCHAR(255),
    isin_code VARCHAR(255),
    market VARCHAR(255),
    check_dt DATE
);

CREATE TABLE TB_CORP_DETAIL (
    corp_code VARCHAR(255) PRIMARY KEY,
    national VARCHAR(255),
    state VARCHAR(255),
    corp_type VARCHAR(255)
);
