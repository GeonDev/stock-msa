-- 기존 테이블 재생성 (복합키 → auto-increment PK)
DROP TABLE IF EXISTS TB_CORP_FINANCE_INDICATOR;
DROP TABLE IF EXISTS TB_CORP_FINANCE;

CREATE TABLE TB_CORP_FINANCE (
    id BIGINT NOT NULL AUTO_INCREMENT,
    corp_code VARCHAR(255) NOT NULL COMMENT '법인번호',
    biz_year VARCHAR(4) NOT NULL COMMENT '사업연도',
    report_code VARCHAR(5) NOT NULL DEFAULT '11011' COMMENT '보고서코드 (11013:1Q, 11012:반기, 11014:3Q, 11011:연간)',
    bas_dt DATE NOT NULL COMMENT '기준일자',
    validation_status VARCHAR(50) COMMENT '검증상태',
    currency VARCHAR(10) COMMENT '통화코드',
    revenue DECIMAL(25, 4) COMMENT '매출액',
    op_income DECIMAL(25, 4) COMMENT '영업이익',
    net_income DECIMAL(25, 4) COMMENT '순이익',
    depreciation DECIMAL(25, 4) COMMENT '감가상각비',
    total_asset DECIMAL(25, 4) COMMENT '총자산',
    total_debt DECIMAL(25, 4) COMMENT '총부채',
    total_capital DECIMAL(25, 4) COMMENT '총자본',
    operating_cashflow DECIMAL(25, 4) COMMENT '영업활동 현금흐름',
    investing_cashflow DECIMAL(25, 4) COMMENT '투자활동 현금흐름',
    financing_cashflow DECIMAL(25, 4) COMMENT '재무활동 현금흐름',
    free_cashflow DECIMAL(25, 4) COMMENT '잉여현금흐름',
    ebitda DECIMAL(25, 4) COMMENT 'EBITDA',
    PRIMARY KEY (id),
    UNIQUE KEY uq_corp_year_report (corp_code, biz_year, report_code),
    INDEX idx_bas_dt (bas_dt),
    INDEX idx_year_report (biz_year, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기업 재무 정보';

CREATE TABLE TB_CORP_FINANCE_INDICATOR (
    id BIGINT NOT NULL AUTO_INCREMENT,
    corp_finance_id BIGINT COMMENT '재무 정보 ID (FK)',
    corp_code VARCHAR(255) NOT NULL COMMENT '법인번호',
    bas_dt DATE COMMENT '기준일자',
    report_code VARCHAR(5) COMMENT '보고서코드',
    per DECIMAL(25, 4) COMMENT 'PER',
    pbr DECIMAL(25, 4) COMMENT 'PBR',
    psr DECIMAL(25, 4) COMMENT 'PSR',
    pcr DECIMAL(25, 4) COMMENT 'PCR',
    ev_ebitda DECIMAL(25, 4) COMMENT 'EV/EBITDA',
    roe DECIMAL(25, 4) COMMENT 'ROE (%)',
    roa DECIMAL(25, 4) COMMENT 'ROA (%)',
    operating_margin DECIMAL(25, 4) COMMENT '영업이익률 (%)',
    net_margin DECIMAL(25, 4) COMMENT '순이익률 (%)',
    fcf_yield DECIMAL(25, 4) COMMENT 'FCF Yield (%)',
    qoq_revenue_growth DECIMAL(25, 4) COMMENT 'QoQ 매출 성장률 (%)',
    qoq_op_income_growth DECIMAL(25, 4) COMMENT 'QoQ 영업이익 성장률 (%)',
    qoq_net_income_growth DECIMAL(25, 4) COMMENT 'QoQ 순이익 성장률 (%)',
    yoy_revenue_growth DECIMAL(25, 4) COMMENT 'YoY 매출 성장률 (%)',
    yoy_op_income_growth DECIMAL(25, 4) COMMENT 'YoY 영업이익 성장률 (%)',
    yoy_net_income_growth DECIMAL(25, 4) COMMENT 'YoY 순이익 성장률 (%)',
    PRIMARY KEY (id),
    INDEX idx_corp_code (corp_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재무 지표';
