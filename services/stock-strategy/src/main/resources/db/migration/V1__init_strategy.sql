-- 백테스팅 시뮬레이션
CREATE TABLE TB_BACKTEST_SIMULATION (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    strategy_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    initial_capital DECIMAL(19, 2) NOT NULL,
    rebalancing_period VARCHAR(20) NOT NULL,
    trading_fee_rate DECIMAL(10, 6) NOT NULL,
    tax_rate DECIMAL(10, 6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 포트폴리오 스냅샷
CREATE TABLE TB_PORTFOLIO_SNAPSHOT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_value DECIMAL(19, 2) NOT NULL,
    cash_balance DECIMAL(19, 2) NOT NULL,
    holdings JSON,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id),
    INDEX idx_simulation_date (simulation_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 백테스팅 결과
CREATE TABLE TB_BACKTEST_RESULT (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL UNIQUE,
    final_value DECIMAL(19, 2) NOT NULL,
    total_return DECIMAL(10, 4),
    cagr DECIMAL(10, 4),
    mdd DECIMAL(10, 4),
    sharpe_ratio DECIMAL(10, 4),
    volatility DECIMAL(10, 4),
    win_rate DECIMAL(10, 4),
    total_trades INT,
    profitable_trades INT,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 매매 이력
CREATE TABLE TB_TRADE_HISTORY (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    simulation_id BIGINT NOT NULL,
    trade_date DATE NOT NULL,
    stock_code VARCHAR(10) NOT NULL,
    order_type VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    fee DECIMAL(19, 2) NOT NULL,
    tax DECIMAL(19, 2) NOT NULL,
    FOREIGN KEY (simulation_id) REFERENCES TB_BACKTEST_SIMULATION(id),
    INDEX idx_simulation_date (simulation_id, trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
