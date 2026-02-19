CREATE TABLE TB_CORP_INFO (
    corp_code VARCHAR(255) PRIMARY KEY,
    corp_name VARCHAR(255),
    stock_code VARCHAR(255),
    isin_code VARCHAR(255),
    dart_corp_code VARCHAR(8) COMMENT 'DART 고유번호 (8자리)',
    market VARCHAR(255),
    check_dt DATE
);

CREATE INDEX idx_dart_corp_code ON TB_CORP_INFO(dart_corp_code);

CREATE TABLE TB_CORP_DETAIL (
    corp_code VARCHAR(255) PRIMARY KEY,
    national VARCHAR(255),
    state VARCHAR(255),
    corp_type VARCHAR(255),
    sector VARCHAR(50)
);
