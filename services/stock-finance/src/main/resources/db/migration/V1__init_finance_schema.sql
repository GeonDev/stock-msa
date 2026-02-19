-- 재무 정보 테이블
CREATE TABLE TB_CORP_FINANCE (
    corp_code VARCHAR(255) NOT NULL COMMENT '법인번호',
    biz_year VARCHAR(4) NOT NULL COMMENT '사업연도',
    report_code VARCHAR(5) NOT NULL DEFAULT '11011' COMMENT '보고서코드 (11013:1Q, 11012:반기, 11014:3Q, 11011:연간)',
    bas_dt DATE NOT NULL COMMENT '기준일자',
    validation_status VARCHAR(50) COMMENT '검증상태',
    currency VARCHAR(10) COMMENT '통화코드',
    
    -- 손익계산서
    revenue DECIMAL(25, 4) COMMENT '매출액',
    op_income DECIMAL(25, 4) COMMENT '영업이익',
    net_income DECIMAL(25, 4) COMMENT '순이익',
    depreciation DECIMAL(25, 4) COMMENT '감가상각비',
    
    -- 재무상태표
    total_asset DECIMAL(25, 4) COMMENT '총자산',
    total_debt DECIMAL(25, 4) COMMENT '총부채',
    total_capital DECIMAL(25, 4) COMMENT '총자본',
    
    -- 현금흐름표
    operating_cashflow DECIMAL(25, 4) COMMENT '영업활동 현금흐름',
    investing_cashflow DECIMAL(25, 4) COMMENT '투자활동 현금흐름',
    financing_cashflow DECIMAL(25, 4) COMMENT '재무활동 현금흐름',
    
    -- 계산 지표
    free_cashflow DECIMAL(25, 4) COMMENT '잉여현금흐름 (FCF = 영업CF - 투자CF)',
    ebitda DECIMAL(25, 4) COMMENT 'EBITDA (영업이익 + 감가상각비)',
    
    PRIMARY KEY (corp_code, biz_year, report_code),
    INDEX idx_bas_dt (bas_dt),
    INDEX idx_year_report (biz_year, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기업 재무 정보';

-- 재무 지표 테이블
CREATE TABLE TB_CORP_FINANCE_INDICATOR (
    corp_code VARCHAR(255) NOT NULL COMMENT '법인번호',
    bas_dt DATE NOT NULL COMMENT '기준일자',
    report_code VARCHAR(5) NOT NULL DEFAULT '11011' COMMENT '보고서코드',
    
    -- 가치평가 지표
    per DECIMAL(25, 4) COMMENT 'PER (주가수익비율)',
    pbr DECIMAL(25, 4) COMMENT 'PBR (주가순자산비율)',
    psr DECIMAL(25, 4) COMMENT 'PSR (주가매출비율)',
    pcr DECIMAL(25, 4) COMMENT 'PCR (주가현금흐름비율)',
    ev_ebitda DECIMAL(25, 4) COMMENT 'EV/EBITDA',
    
    -- 수익성 지표
    roe DECIMAL(25, 4) COMMENT 'ROE (자기자본이익률, %)',
    roa DECIMAL(25, 4) COMMENT 'ROA (총자산이익률, %)',
    operating_margin DECIMAL(25, 4) COMMENT '영업이익률 (%)',
    net_margin DECIMAL(25, 4) COMMENT '순이익률 (%)',
    fcf_yield DECIMAL(25, 4) COMMENT 'FCF Yield (잉여현금흐름 수익률, %)',
    
    -- 분기별 성장률 (QoQ)
    qoq_revenue_growth DECIMAL(25, 4) COMMENT 'QoQ 매출 성장률 (%)',
    qoq_op_income_growth DECIMAL(25, 4) COMMENT 'QoQ 영업이익 성장률 (%)',
    qoq_net_income_growth DECIMAL(25, 4) COMMENT 'QoQ 순이익 성장률 (%)',
    
    -- 전년 동기 대비 성장률 (YoY)
    yoy_revenue_growth DECIMAL(25, 4) COMMENT 'YoY 매출 성장률 (%)',
    yoy_op_income_growth DECIMAL(25, 4) COMMENT 'YoY 영업이익 성장률 (%)',
    yoy_net_income_growth DECIMAL(25, 4) COMMENT 'YoY 순이익 성장률 (%)',
    
    PRIMARY KEY (corp_code, bas_dt, report_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='재무 지표';
