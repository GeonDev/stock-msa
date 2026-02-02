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
