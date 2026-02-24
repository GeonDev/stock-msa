CREATE TABLE TB_SECTOR_ANALYSIS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sector_name VARCHAR(50) NOT NULL,
    analysis_date DATE NOT NULL,
    avg_momentum_12m DECIMAL(10, 4),
    relative_strength DECIMAL(10, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sector_date (sector_name, analysis_date)
);

CREATE TABLE TB_FACTOR_SCORE (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL,
    score_date DATE NOT NULL,
    value_score DECIMAL(10, 4),
    momentum_score DECIMAL(10, 4),
    quality_score DECIMAL(10, 4),
    total_score DECIMAL(10, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_date (stock_code, score_date)
);
