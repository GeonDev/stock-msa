CREATE TABLE TB_CORP_FINANCE (
    corp_code VARCHAR(255),
    bas_dt DATE NOT NULL,
    biz_year VARCHAR(255),
    currency VARCHAR(255),
    op_income DECIMAL(25, 4),
    investment DECIMAL(25, 4),
    net_income DECIMAL(25, 4),
    revenue DECIMAL(25, 4),
    total_asset DECIMAL(25, 4),
    total_debt DECIMAL(25, 4),
    total_capital DECIMAL(25, 4),
    doc_code VARCHAR(255),
    doc_name VARCHAR(255),
    doc_debt_ratio DECIMAL(25, 4),
    income_before_tax DECIMAL(25, 4),
    prev_op_income DECIMAL(25, 4),
    prev_net_income DECIMAL(25, 4),
    prev_revenue DECIMAL(25, 4),
    validation_status VARCHAR(50),
    PRIMARY KEY (corp_code, bas_dt)
);

CREATE TABLE TB_CORP_FINANCE_INDICATOR (
    corp_code VARCHAR(255),
    bas_dt DATE NOT NULL,
    per DECIMAL(25, 4),
    pbr DECIMAL(25, 4),
    psr DECIMAL(25, 4),
    revenue_growth DECIMAL(25, 4),
    net_income_growth DECIMAL(25, 4),
    op_income_growth DECIMAL(25, 4),
    roe DECIMAL(25, 4),
    roa DECIMAL(25, 4),
    debt_ratio DECIMAL(25, 4),
    PRIMARY KEY (corp_code, bas_dt)
);
